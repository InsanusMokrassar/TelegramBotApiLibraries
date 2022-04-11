package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent
import dev.inmo.tgbotapi.utils.StorageFile
import io.ktor.utils.io.core.*

interface MessagesSimpleCache {
    suspend fun set(
        chatId: ChatId,
        messageIdentifier: MessageIdentifier,
        content: MessageContent
    )
    suspend fun get(chatId: ChatId, messageIdentifier: MessageIdentifier): MessageContent?
    suspend fun remove(chatId: ChatId, messageIdentifier: MessageIdentifier)
    suspend fun contains(chatId: ChatId, messageIdentifier: MessageIdentifier): Boolean
}

/**
 * It is not recommended to use in production realization of [MessagesFilesCache] which has been created for fast
 * start of application creation with usage of [MessageContentCache] with aim to replace this realization by some
 * disks-oriented one
 */
class InMemoryMessagesSimpleCache : MessagesSimpleCache {
    private val map = mutableMapOf<Pair<ChatId, MessageIdentifier>, MessageContent>()

    override suspend fun set(
        chatId: ChatId,
        messageIdentifier: MessageIdentifier,
        content: MessageContent
    ) {
        map[chatId to messageIdentifier] = content
    }

    override suspend fun get(chatId: ChatId, messageIdentifier: MessageIdentifier): MessageContent? {
        return map[chatId to messageIdentifier]
    }

    override suspend fun remove(chatId: ChatId, messageIdentifier: MessageIdentifier) {
        map.remove(chatId to messageIdentifier)
    }

    override suspend fun contains(chatId: ChatId, messageIdentifier: MessageIdentifier): Boolean {
        return map.contains(chatId to messageIdentifier)
    }

}
