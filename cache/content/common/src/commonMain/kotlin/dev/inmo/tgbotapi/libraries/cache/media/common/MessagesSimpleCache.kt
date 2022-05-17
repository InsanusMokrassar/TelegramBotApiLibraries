package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.types.message.content.MessageContent

interface MessagesSimpleCache<K> {
    suspend fun set(k: K, content: MessageContent)
    suspend fun update(k: K, content: MessageContent): Boolean
    suspend fun get(k: K): MessageContent?
    suspend fun remove(k: K)
    suspend fun contains(k: K): Boolean
}

/**
 * It is not recommended to use in production realization of [MessagesFilesCache] which has been created for fast
 * start of application creation with usage of [MessageContentCache] with aim to replace this realization by some
 * disks-oriented one
 */
class InMemoryMessagesSimpleCache<K> : MessagesSimpleCache<K> {
    private val map = mutableMapOf<K, MessageContent>()

    override suspend fun set(
        k: K,
        content: MessageContent
    ) {
        map[k] = content
    }

    override suspend fun update(
        k: K,
        content: MessageContent
    ): Boolean {
        return map.runCatching {
            if (contains(k)) {
                put(k, content)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    override suspend fun get(k: K): MessageContent? {
        return map[k]
    }

    override suspend fun remove(k: K) {
        map.remove(k)
    }

    override suspend fun contains(k: K): Boolean {
        return map.contains(k)
    }
}
