package dev.inmo.tgbotapi.libraries.cache.admins.micro_utils

import com.soywiz.klock.DateTime
import dev.inmo.micro_utils.coroutines.actor
import dev.inmo.micro_utils.coroutines.safelyWithoutExceptions
import dev.inmo.micro_utils.repos.*
import dev.inmo.tgbotapi.libraries.cache.admins.DefaultAdminsCacheAPIRepo
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.*

private sealed class RepoActions<T> {
    abstract val toReturn: Continuation<T>
}
private class GetUpdateDateTimeRepoAction(
    val chatId: ChatId,
    override val toReturn: Continuation<DateTime?>
) : RepoActions<DateTime?>()
private class GetChatAdminsRepoAction(
    val chatId: ChatId,
    override val toReturn: Continuation<List<AdministratorChatMember>?>
) : RepoActions<List<AdministratorChatMember>?>()
private class SetChatAdminsRepoAction(
    val chatId: ChatId,
    val newValue: List<AdministratorChatMember>,
    override val toReturn: Continuation<Unit>
) : RepoActions<Unit>()

class DefaultAdminsCacheAPIRepo(
    private val adminsRepo: KeyValuesRepo<ChatId, AdministratorChatMember>,
    private val updatesRepo: KeyValueRepo<ChatId, MilliSeconds>,
    private val scope: CoroutineScope
) : DefaultAdminsCacheAPIRepo {
    private val actor = scope.actor<RepoActions<*>>(Channel.UNLIMITED) {
        safelyWithoutExceptions {
            when (it) {
                is GetUpdateDateTimeRepoAction -> it.toReturn.resume(
                    updatesRepo.get(it.chatId) ?.let { DateTime(it.toDouble()) }
                )
                is GetChatAdminsRepoAction -> it.toReturn.resume(adminsRepo.getAll(it.chatId))
                is SetChatAdminsRepoAction -> {
                    adminsRepo.clear(it.chatId)
                    adminsRepo.set(it.chatId, it.newValue)
                    updatesRepo.set(it.chatId, DateTime.now().unixMillisLong)
                    it.toReturn.resume(Unit)
                }
            }
        }
    }

    override suspend fun getChatAdmins(chatId: ChatId): List<AdministratorChatMember>? = suspendCoroutine {
        actor.trySend(GetChatAdminsRepoAction(chatId, it))
    }
    override suspend fun setChatAdmins(chatId: ChatId, chatMembers: List<AdministratorChatMember>) = suspendCoroutine<Unit> {
        actor.trySend(SetChatAdminsRepoAction(chatId, chatMembers, it))
    }
    override suspend fun lastUpdate(chatId: ChatId): DateTime? = suspendCoroutine {
        actor.trySend(GetUpdateDateTimeRepoAction(chatId, it))
    }
}
