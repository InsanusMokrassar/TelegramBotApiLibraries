package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.MessageContent
import io.ktor.utils.io.core.Input

interface MessageContentCache<K> {
    suspend fun save(k: K, content: MessageContent)
    suspend fun save(
        k: K,
        content: MediaContent,
        filename: String,
        inputAllocator: suspend () -> Input
    )

    suspend fun get(k: K): MessageContent?
    suspend fun contains(k: K): Boolean
    suspend fun remove(k: K)
}
