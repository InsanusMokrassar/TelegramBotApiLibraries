package dev.inmo.tgbotapi.libraries.cache.admins

import korlibs.time.DateTime
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.chat.ExtendedBot
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.message.abstracts.*
import korlibs.time.seconds
import kotlinx.serialization.Serializable

interface DefaultAdminsCacheAPIRepo {
    suspend fun getChatAdmins(chatId: IdChatIdentifier): List<AdministratorChatMember>?
    suspend fun setChatAdmins(chatId: IdChatIdentifier, chatMembers: List<AdministratorChatMember>)
    suspend fun lastUpdate(chatId: IdChatIdentifier): DateTime?
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

    override suspend fun getChatAdmins(chatId: IdChatIdentifier): List<AdministratorChatMember>? {
        val settings = settingsAPI.getChatSettings(chatId)
        val lastUpdate = repo.lastUpdate(chatId)
        return when {
            settings == null -> null
            settings.refreshOnCacheCalls &&
                (lastUpdate == null || (DateTime.now() - lastUpdate).seconds > settings.refreshSeconds) -> {
                bot.updateAdmins(chatId, repo, getBotInfo())
            }
            else -> repo.getChatAdmins(chatId) ?: bot.updateAdmins(chatId, repo, getBotInfo())
        }
    }

    override suspend fun isAdmin(chatId: IdChatIdentifier, userId: UserId): Boolean {
        val settings = settingsAPI.getChatSettings(chatId)
        val lastUpdate = repo.lastUpdate(chatId)
        return when {
            settings == null -> return false
            settings.refreshOnCacheCalls && (lastUpdate == null || (DateTime.now() - lastUpdate).seconds > settings.refreshSeconds) -> {
                bot.updateAdmins(chatId, repo, getBotInfo())
            }
            else -> {
                val chatAdmins = repo.getChatAdmins(chatId)
                if (chatAdmins == null) {
                    return bot.getChatMember(chatId, userId) is AdministratorChatMember
                }
                chatAdmins
            }
        }.any { it.user.id == userId }
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
