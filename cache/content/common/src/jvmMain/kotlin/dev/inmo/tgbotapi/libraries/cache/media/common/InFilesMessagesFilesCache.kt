package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.copyTo
import io.ktor.utils.io.streams.*
import java.io.File

class InFilesMessagesFilesCache<K>(
    private val folderFile: File,
    private val filePrefixBuilder: (K) -> String
) : MessagesFilesCache<K> {
    private val K.multipartFile: MultipartFile?
        get() {
            val prefix = filePrefix(this)
            val filename = folderFile.list() ?.firstOrNull { it.startsWith(prefix) } ?: return null
            val file = File(folderFile, filename)
            val storageFileFilename = file.name.removePrefix("$prefix ")

            return MultipartFile(storageFileFilename) {
                file.inputStream().asInput()
            }
        }

    init {
        require(!folderFile.isFile) { "Folder of messages files cache can't be file, but was $folderFile" }
        folderFile.mkdirs()
    }

    private fun filePrefix(k: K): String = filePrefixBuilder(k)

    private fun fileName(k: K, filename: String): String {
        return "${filePrefix(k)} $filename"
    }

    override suspend fun set(k: K, filename: String, inputAllocator: suspend () -> Input) {
        val fullFileName = fileName(k, filename)
        val file = File(folderFile, fullFileName).apply {
            delete()
        }
        inputAllocator().inputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    override suspend fun get(k: K): MultipartFile? {
        return k.multipartFile
    }

    override suspend fun remove(k: K) {
        val prefix = filePrefix(k)
        folderFile.listFiles() ?.forEach {
            if (it.name.startsWith(prefix)) {
                it.delete()
            }
        }
    }

    override suspend fun contains(k: K): Boolean {
        val prefix = filePrefix(k)
        return folderFile.list() ?.any { it.startsWith(prefix) } == true
    }

    companion object {
        operator fun invoke(folderFile: File) = InFilesMessagesFilesCache<String>(
            folderFile
        ) { it }
    }
}
