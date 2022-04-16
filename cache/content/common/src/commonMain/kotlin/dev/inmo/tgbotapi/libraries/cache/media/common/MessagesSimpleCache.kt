package dev.inmo.tgbotapi.libraries.cache.media.common

import com.benasher44.uuid.uuid4
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent

interface MessagesSimpleCache<K> {
    suspend fun add(content: MessageContent): K
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
class InMemoryMessagesSimpleCache<K>(
    private val keyGenerator: () -> K
) : MessagesSimpleCache<K> {
    private val map = mutableMapOf<K, MessageContent>()

    override suspend fun add(
        content: MessageContent
    ): K {
        val key = keyGenerator()
        map[key] = content
        return key
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

    companion object {
        operator fun invoke() = InMemoryMessagesSimpleCache {
            uuid4().toString()
        }
    }
}
