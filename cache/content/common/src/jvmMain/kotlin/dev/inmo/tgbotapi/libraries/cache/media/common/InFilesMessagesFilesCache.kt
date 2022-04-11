package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.utils.*
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.copyTo
import io.ktor.utils.io.streams.asInput
import io.ktor.utils.io.streams.asOutput
import java.io.File

class InFilesMessagesFilesCache(
    private val folderFile: File
) : MessagesFilesCache {
    private val Pair<ChatId, MessageIdentifier>.storageFile: StorageFile?
        get() {
            val prefix = filePrefix(first, second)
            val filename = folderFile.list() ?.firstOrNull { it.startsWith(prefix) } ?: return null
            val file = File(folderFile, filename)
            val storageFileFilename = file.name.removePrefix("$prefix ")

            return StorageFile(
                StorageFileInfo(storageFileFilename)
            ) {
                file.inputStream().asInput()
            }
        }

    init {
        require(!folderFile.isFile) { "Folder of messages files cache can't be file, but was $folderFile" }
        folderFile.mkdirs()
    }

    private fun filePrefix(chatId: ChatId, messageIdentifier: MessageIdentifier): String {
        return "${chatId.chatId} $messageIdentifier"
    }

    private fun fileName(chatId: ChatId, messageIdentifier: MessageIdentifier, filename: String): String {
        return "${chatId.chatId} $messageIdentifier $filename"
    }

    override suspend fun set(
        chatId: ChatId,
        messageIdentifier: MessageIdentifier,
        filename: String,
        inputAllocator: suspend () -> Input
    ) {
        val fullFileName = fileName(chatId, messageIdentifier, filename)
        val file = File(folderFile, fullFileName).apply {
            delete()
        }
        inputAllocator().use { input ->
            file.outputStream().asOutput().use { output ->
                input.copyTo(output)
            }
        }
    }

    override suspend fun get(chatId: ChatId, messageIdentifier: MessageIdentifier): StorageFile? {
        return (chatId to messageIdentifier).storageFile
    }

    override suspend fun remove(chatId: ChatId, messageIdentifier: MessageIdentifier) {
        val prefix = filePrefix(chatId, messageIdentifier)
        folderFile.listFiles() ?.forEach {
            if (it.name.startsWith(prefix)) {
                it.delete()
            }
        }
    }

    override suspend fun contains(chatId: ChatId, messageIdentifier: MessageIdentifier): Boolean {
        val prefix = filePrefix(chatId, messageIdentifier)
        return folderFile.list() ?.any { it.startsWith(prefix) } == true
    }
}
