package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.tgbotapi.extensions.utils.asGroupContentMessage
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.ChatMember.abstracts.AdministratorChatMember
import dev.inmo.tgbotapi.types.message.abstracts.GroupContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message

interface AdminsCacheAPI {
    suspend fun getChatAdmins(chatId: ChatId): List<AdministratorChatMember>?
    suspend fun isAdmin(chatId: ChatId, userId: UserId): Boolean = getChatAdmins(chatId) ?.any {
        it.user.id == userId
    } == true
    suspend fun sentByAdmin(groupContentMessage: GroupContentMessage<*>): Boolean
    suspend fun sentByAdmin(message: Message): Boolean? {
        return sentByAdmin(message.asGroupContentMessage() ?: return null)
    }

    suspend fun settings(): AdminsCacheSettingsAPI
}
