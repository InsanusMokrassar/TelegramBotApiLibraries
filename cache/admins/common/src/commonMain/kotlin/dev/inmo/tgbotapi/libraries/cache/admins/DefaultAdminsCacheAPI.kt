package dev.inmo.tgbotapi.libraries.cache.admins

import com.soywiz.klock.DateTime
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.chat.ExtendedBot
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.message.abstracts.*
import kotlinx.serialization.Serializable

interface DefaultAdminsCacheAPIRepo {
    suspend fun getChatAdmins(chatId: ChatId): List<AdministratorChatMember>?
    suspend fun setChatAdmins(chatId: ChatId, chatMembers: List<AdministratorChatMember>)
    suspend fun lastUpdate(chatId: ChatId): DateTime?
}

@Serializable
class DefaultAdminsCacheAPI(
    private val bot: TelegramBot,
    private val repo: DefaultAdminsCacheAPIRepo,
    private val settingsAPI: AdminsCacheSettingsAPI
) : AdminsCacheAPI {
    private lateinit var botInfo: ExtendedBot
    private suspend fun getBotInfo(): ExtendedBot = try {
        botInfo
    } catch (e: Throwable) {
        bot.getMe().also { botInfo = it }
    }

    override suspend fun getChatAdmins(chatId: ChatId): List<AdministratorChatMember>? {
        val settings = settingsAPI.getChatSettings(chatId)
        val lastUpdate = repo.lastUpdate(chatId)
        return when {
            settings == null -> null
            settings.refreshOnRequests &&
                (lastUpdate == null || (DateTime.now() - lastUpdate).seconds > settings.refreshSeconds) -> {
                bot.updateAdmins(chatId, repo, getBotInfo())
            }
            else -> repo.getChatAdmins(chatId) ?: bot.updateAdmins(chatId, repo, getBotInfo())
        }
    }

    override suspend fun sentByAdmin(groupContentMessage: GroupContentMessage<*>): Boolean {
        return when (groupContentMessage) {
            is AnonymousGroupContentMessage -> true
            is CommonGroupContentMessage -> isAdmin(
                groupContentMessage.chat.id,
                groupContentMessage.user.id
            )
            else -> false
        }
    }

    override suspend fun settings(): AdminsCacheSettingsAPI = settingsAPI
}
