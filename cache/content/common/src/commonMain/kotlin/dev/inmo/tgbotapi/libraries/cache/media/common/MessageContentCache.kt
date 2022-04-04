package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent

interface MessageContentCache {
    suspend fun save(chatId: ChatId, messageId: MessageIdentifier, content: MessageContent): Boolean
    suspend fun get(chatId: ChatId, messageId: MessageIdentifier): MessageContent?
    suspend fun contains(chatId: ChatId, messageId: MessageIdentifier): Boolean
    suspend fun remove(chatId: ChatId, messageId: MessageIdentifier)
}