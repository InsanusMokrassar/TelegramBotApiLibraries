package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.utils.StorageFile
import io.ktor.utils.io.core.*

interface MessagesFilesCache {
    suspend fun set(
        chatId: ChatId,
        messageIdentifier: MessageIdentifier,
        filename: String,
        inputAllocator: suspend () -> Input
    )
    suspend fun get(chatId: ChatId, messageIdentifier: MessageIdentifier): StorageFile?
    suspend fun remove(chatId: ChatId, messageIdentifier: MessageIdentifier)
    suspend fun contains(chatId: ChatId, messageIdentifier: MessageIdentifier): Boolean
}

/**
 * It is not recommended to use in production realization of [MessagesFilesCache] which has been created for fast
 * start of application creation with usage of [MessageContentCache] with aim to replace this realization by some
 * disks-oriented one
 */
class InMemoryMessagesFilesCache : MessagesFilesCache {
    private val map = mutableMapOf<Pair<ChatId, MessageIdentifier>, StorageFile>()

    override suspend fun set(
        chatId: ChatId,
        messageIdentifier: MessageIdentifier,
        filename: String,
        inputAllocator: suspend () -> Input
    ) {
        map[chatId to messageIdentifier] = StorageFile(
            filename,
            inputAllocator().readBytes()
        )
    }

    override suspend fun get(chatId: ChatId, messageIdentifier: MessageIdentifier): StorageFile? {
        return map[chatId to messageIdentifier]
    }

    override suspend fun remove(chatId: ChatId, messageIdentifier: MessageIdentifier) {
        map.remove(chatId to messageIdentifier)
    }

    override suspend fun contains(chatId: ChatId, messageIdentifier: MessageIdentifier): Boolean {
        return map.contains(chatId to messageIdentifier)
    }

}
