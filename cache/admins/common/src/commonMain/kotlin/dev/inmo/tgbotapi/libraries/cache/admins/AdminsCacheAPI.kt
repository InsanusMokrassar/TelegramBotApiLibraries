package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.ChatMember.abstracts.AdministratorChatMember

interface AdminsCacheAPI {
    suspend fun getChatAdmins(chatId: ChatId): List<AdministratorChatMember>?
    suspend fun isAdmin(userId: UserId, chatId: ChatId): Boolean

    suspend fun settings(): AdminsCacheSettingsAPI
}
