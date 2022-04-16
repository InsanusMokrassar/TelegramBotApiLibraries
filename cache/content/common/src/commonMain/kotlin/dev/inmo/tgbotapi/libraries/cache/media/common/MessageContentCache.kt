package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.types.message.content.abstracts.MediaContent
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent
import io.ktor.utils.io.core.Input

interface MessageContentCache<K> {
    suspend fun save(content: MessageContent): K
    suspend fun save(
        content: MediaContent,
        filename: String,
        inputAllocator: suspend () -> Input
    ): K
    suspend fun get(k: K): MessageContent?
    suspend fun contains(k: K): Boolean
    suspend fun remove(k: K)
}
