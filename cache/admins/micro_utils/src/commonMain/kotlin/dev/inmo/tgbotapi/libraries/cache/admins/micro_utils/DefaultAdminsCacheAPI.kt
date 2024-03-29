package dev.inmo.tgbotapi.libraries.cache.admins.micro_utils

import korlibs.time.DateTime
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.micro_utils.repos.*
import dev.inmo.tgbotapi.libraries.cache.admins.DefaultAdminsCacheAPIRepo
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*

private sealed class RepoActions<T> {
    abstract val deferred: CompletableDeferred<T>
}
private class GetUpdateDateTimeRepoAction(
    val chatId: IdChatIdentifier,
    override val deferred: CompletableDeferred<DateTime?>
) : RepoActions<DateTime?>()
private class GetChatAdminsRepoAction(
    val chatId: IdChatIdentifier,
    override val deferred: CompletableDeferred<List<AdministratorChatMember>?>
) : RepoActions<List<AdministratorChatMember>?>()
private class SetChatAdminsRepoAction(
    val chatId: IdChatIdentifier,
    val newValue: List<AdministratorChatMember>,
    override val deferred: CompletableDeferred<Unit>
) : RepoActions<Unit>()

class DefaultAdminsCacheAPIRepoImpl(
    private val adminsRepo: KeyValuesRepo<IdChatIdentifier, AdministratorChatMember>,
    private val updatesRepo: KeyValueRepo<IdChatIdentifier, MilliSeconds>,
    private val scope: CoroutineScope
) : DefaultAdminsCacheAPIRepo {
    private val actor = scope.actorAsync<RepoActions<*>>(Channel.UNLIMITED) {
        safelyWithoutExceptions(
            { e ->
                it.deferred.completeExceptionally(e)
            }
        ) {
            when (it) {
                is GetUpdateDateTimeRepoAction -> it.deferred.complete(
                    updatesRepo.get(it.chatId) ?.let { DateTime(it.toDouble()) }
                )
                is GetChatAdminsRepoAction -> it.deferred.complete(adminsRepo.getAll(it.chatId))
                is SetChatAdminsRepoAction -> {
                    adminsRepo.clear(it.chatId)
                    adminsRepo.set(it.chatId, it.newValue)
                    updatesRepo.set(it.chatId, DateTime.now().unixMillisLong)
                    it.deferred.complete(Unit)
                }
            }
        }
    }

    override suspend fun getChatAdmins(chatId: IdChatIdentifier): List<AdministratorChatMember>? {
        val deferred = CompletableDeferred<List<AdministratorChatMember>?>()
        actor.trySend(
            GetChatAdminsRepoAction(chatId, deferred)
        ).onFailure {
            deferred.completeExceptionally(it ?: IllegalStateException("Something went wrong when tried to add getChatAdmins action"))
        }
        return deferred.await()
    }

    override suspend fun setChatAdmins(chatId: IdChatIdentifier, chatMembers: List<AdministratorChatMember>) {
        val deferred = CompletableDeferred<Unit>()
        actor.trySend(
            SetChatAdminsRepoAction(chatId, chatMembers, deferred)
        ).onFailure {
            deferred.completeExceptionally(it ?: IllegalStateException("Something went wrong when tried to add setChatAdmins action"))
        }
        return deferred.await()
    }
    override suspend fun lastUpdate(chatId: IdChatIdentifier): DateTime? {
        val deferred = CompletableDeferred<DateTime?>()
        actor.trySend(
            GetUpdateDateTimeRepoAction(chatId, deferred)
        ).onFailure {
            deferred.completeExceptionally(it ?: IllegalStateException("Something went wrong when tried to add lastUpdate action"))
        }
        return deferred.await()
    }
}

fun DefaultAdminsCacheAPIRepo(
    adminsRepo: KeyValuesRepo<IdChatIdentifier, AdministratorChatMember>,
    updatesRepo: KeyValueRepo<IdChatIdentifier, MilliSeconds>,
    scope: CoroutineScope
) = DefaultAdminsCacheAPIRepoImpl(adminsRepo, updatesRepo, scope)
