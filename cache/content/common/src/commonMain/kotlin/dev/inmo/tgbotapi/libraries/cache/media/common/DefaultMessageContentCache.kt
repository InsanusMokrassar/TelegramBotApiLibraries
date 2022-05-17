package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.DownloadFileStream
import dev.inmo.tgbotapi.requests.get.GetFile
import dev.inmo.tgbotapi.requests.send.media.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.media.*
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.asInput
import io.ktor.utils.io.core.Input

class DefaultMessageContentCache<K>(
    private val bot: TelegramBot,
    private val filesRefreshingChatId: ChatId,
    private val simpleMessageContentCache: MessagesSimpleCache<K>,
    private val mediaFileActualityChecker: MediaFileActualityChecker = MediaFileActualityChecker.WithDelay(
        MediaFileActualityChecker.Default(filesRefreshingChatId)
    ),
    private val messagesFilesCache: MessagesFilesCache<K> = InMemoryMessagesFilesCache()
) : MessageContentCache<K> {
    override suspend fun save(k: K, content: MessageContent) {
        when (content) {
            is MediaContent -> {
                val extendedInfo = bot.execute(
                    GetFile(content.media.fileId)
                )
                val allocator = bot.execute(
                    DownloadFileStream(
                        extendedInfo.filePath
                    )
                )

                save(k, content, extendedInfo.fileName) {
                    allocator.invoke().asInput()
                }
            }
            else -> simpleMessageContentCache.set(k, content)
        }
    }

    override suspend fun save(
        k: K,
        content: MediaContent,
        filename: String,
        inputAllocator: suspend () -> Input
    ) {
        simpleMessageContentCache.set(k, content)
        runCatching {
            messagesFilesCache.set(k, filename, inputAllocator)
        }.onFailure {
            simpleMessageContentCache.remove(k)
        }.onSuccess {
            with(mediaFileActualityChecker) {
                bot.saved(content)
            }
        }
    }

    override suspend fun get(k: K): MessageContent? {
        val savedSimpleContent = simpleMessageContentCache.get(k) ?: return null

        if (savedSimpleContent is MediaContent && !with(mediaFileActualityChecker) { bot.isActual(savedSimpleContent) }) {
            val savedFileContentAllocator = messagesFilesCache.get(k) ?: error("Unexpected absence of $k file for content ($simpleMessageContentCache)")
            val newContent = bot.execute(
                when (savedSimpleContent.asTelegramMedia()) {
                    is TelegramMediaAnimation -> SendAnimation(
                        filesRefreshingChatId,
                        savedFileContentAllocator,
                        disableNotification = true
                    )
                    is TelegramMediaAudio -> SendAudio(
                        filesRefreshingChatId,
                        savedFileContentAllocator,
                        disableNotification = true
                    )
                    is TelegramMediaVideo -> SendVideo(
                        filesRefreshingChatId,
                        savedFileContentAllocator,
                        disableNotification = true
                    )
                    is TelegramMediaDocument -> SendDocument(
                        filesRefreshingChatId,
                        savedFileContentAllocator,
                        disableNotification = true
                    )
                    is TelegramMediaPhoto -> SendPhoto(
                        filesRefreshingChatId,
                        savedFileContentAllocator,
                        disableNotification = true
                    )
                }
            )

            simpleMessageContentCache.update(k, newContent.content)
            return newContent.content
        }
        return savedSimpleContent
    }

    override suspend fun contains(k: K): Boolean {
        return simpleMessageContentCache.contains(k)
    }

    override suspend fun remove(k: K) {
        simpleMessageContentCache.remove(k)
        messagesFilesCache.remove(k)
    }

    companion object {
        operator fun invoke(
            bot: TelegramBot,
            filesRefreshingChatId: ChatId,
            simpleMessageContentCache: MessagesSimpleCache<String> = InMemoryMessagesSimpleCache(),
            mediaFileActualityChecker: MediaFileActualityChecker = MediaFileActualityChecker.WithDelay(
                MediaFileActualityChecker.Default(filesRefreshingChatId)
            ),
            messagesFilesCache: MessagesFilesCache<String> = InMemoryMessagesFilesCache()
        ) = DefaultMessageContentCache(bot, filesRefreshingChatId, simpleMessageContentCache, mediaFileActualityChecker, messagesFilesCache)
    }
}
