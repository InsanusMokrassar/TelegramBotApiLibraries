package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import io.ktor.utils.io.core.*

interface MessagesFilesCache<K> {
    suspend fun set(k: K, filename: String, inputAllocator: suspend () -> Input)
    suspend fun get(k: K): MultipartFile?
    suspend fun remove(k: K)
    suspend fun contains(k: K): Boolean
}

/**
 * It is not recommended to use in production realization of [MessagesFilesCache] which has been created for fast
 * start of application creation with usage of [MessageContentCache] with aim to replace this realization by some
 * disks-oriented one
 */
class InMemoryMessagesFilesCache<K> : MessagesFilesCache<K> {
    private val map = mutableMapOf<K, MultipartFile>()

    override suspend fun set(k: K, filename: String, inputAllocator: suspend () -> Input) {
        val input = inputAllocator()
        map[k] = MultipartFile(
            filename
        ) {
            input
        }
    }

    override suspend fun get(k: K): MultipartFile? {
        return map[k]
    }

    override suspend fun remove(k: K) {
        map.remove(k)
    }

    override suspend fun contains(k: K): Boolean {
        return map.contains(k)
    }
}
