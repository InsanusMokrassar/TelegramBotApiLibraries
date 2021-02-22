package dev.inmo.tgbotapi.libraries.cache.admins

import com.soywiz.klock.DateTime
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.ChatMember.abstracts.AdministratorChatMember
import dev.inmo.tgbotapi.types.UserId
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
    private suspend fun triggerUpdate(chatId: ChatId): List<AdministratorChatMember> {
        val admins = bot.getChatAdministrators(chatId)
        repo.setChatAdmins(chatId, admins)
        return admins
    }

    override suspend fun getChatAdmins(chatId: ChatId): List<AdministratorChatMember>? {
        val settings = settingsAPI.getChatSettings(chatId)
        val lastUpdate = repo.lastUpdate(chatId)
        return when {
            settings == null -> null
            settings.refreshOnRequests &&
                (lastUpdate == null || (DateTime.now() - lastUpdate).seconds > settings.refreshSeconds) -> {
                triggerUpdate(chatId)
            }
            else -> repo.getChatAdmins(chatId) ?: triggerUpdate(chatId)
        }
    }

    override suspend fun isAdmin(userId: UserId, chatId: ChatId): Boolean = getChatAdmins(chatId) ?.any {
        it.user.id == userId
    } == true

    override suspend fun settings(): AdminsCacheSettingsAPI = settingsAPI

}
