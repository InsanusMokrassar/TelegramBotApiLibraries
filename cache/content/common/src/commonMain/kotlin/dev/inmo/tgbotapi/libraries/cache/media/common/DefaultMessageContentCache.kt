package dev.inmo.tgbotapi.libraries.cache.media.common

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.DownloadFileStream
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.requests.get.GetFile
import dev.inmo.tgbotapi.requests.send.media.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.InputMedia.*
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.message.content.abstracts.MediaContent
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent
import dev.inmo.tgbotapi.utils.StorageFile
import dev.inmo.tgbotapi.utils.asInput
import io.ktor.utils.io.cancel
import io.ktor.utils.io.core.Input

class DefaultMessageContentCache(
    private val bot: TelegramBot,
    private val filesRefreshingChatId: ChatId,
    private val simpleMessageContentCache: MessagesSimpleCache = InMemoryMessagesSimpleCache(),
    private val messagesFilesCache: MessagesFilesCache = InMemoryMessagesFilesCache()
) : MessageContentCache {
    override suspend fun save(chatId: ChatId, messageId: MessageIdentifier, content: MessageContent): Boolean {
        return when (content) {
            is MediaContent -> {
                val extendedInfo = bot.execute(
                    GetFile(content.media.fileId)
                )
                val allocator = bot.execute(
                    DownloadFileStream(
                        extendedInfo.filePath
                    )
                )

                save(chatId, messageId, content, extendedInfo.fileName) {
                    allocator.invoke().asInput()
                }
            }
            else -> simpleMessageContentCache.runCatching {
                set(chatId, messageId, content)
            }.isSuccess
        }
    }

    override suspend fun save(
        chatId: ChatId,
        messageId: MessageIdentifier,
        content: MediaContent,
        filename: String,
        inputAllocator: suspend () -> Input
    ): Boolean {
        runCatching {
            messagesFilesCache.set(chatId, messageId, filename, inputAllocator)
        }.onFailure {
            return false
        }

        return simpleMessageContentCache.runCatching {
            set(chatId, messageId, content)
        }.isSuccess
    }

    override suspend fun get(chatId: ChatId, messageId: MessageIdentifier): MessageContent? {
        val savedSimpleContent = simpleMessageContentCache.get(chatId, messageId) ?: return null

        if (savedSimpleContent is MediaContent) {
            runCatching {
                val streamAllocator = bot.execute(
                    DownloadFileStream(
                        bot.execute(
                            GetFile(
                                savedSimpleContent.media.fileId
                            )
                        ).filePath
                    )
                )

                streamAllocator().apply {
                    readByte()
                    cancel()
                }
            }.onFailure {
                val savedFileContentAllocator = messagesFilesCache.get(chatId, messageId) ?: error("Unexpected absence of $chatId:$messageId file for content ($simpleMessageContentCache)")
                val newContent = bot.execute(
                    when (savedSimpleContent.asInputMedia()) {
                        is InputMediaAnimation -> SendAnimation(
                            filesRefreshingChatId,
                            MultipartFile(
                                savedFileContentAllocator
                            ),
                            disableNotification = true
                        )
                        is InputMediaAudio -> SendAudio(
                            filesRefreshingChatId,
                            MultipartFile(
                                savedFileContentAllocator
                            ),
                            disableNotification = true
                        )
                        is InputMediaVideo -> SendVideo(
                            filesRefreshingChatId,
                            MultipartFile(
                                savedFileContentAllocator
                            ),
                            disableNotification = true
                        )
                        is InputMediaDocument -> SendDocument(
                            filesRefreshingChatId,
                            MultipartFile(
                                savedFileContentAllocator
                            ),
                            disableNotification = true
                        )
                        is InputMediaPhoto -> SendPhoto(
                            filesRefreshingChatId,
                            MultipartFile(
                                savedFileContentAllocator
                            ),
                            disableNotification = true
                        )
                    }
                )

                simpleMessageContentCache.set(chatId, messageId, newContent.content)
                return newContent.content
            }
        }
        return savedSimpleContent
    }

    override suspend fun contains(chatId: ChatId, messageId: MessageIdentifier): Boolean {
        return simpleMessageContentCache.contains(chatId, messageId)
    }

    override suspend fun remove(chatId: ChatId, messageId: MessageIdentifier) {
        simpleMessageContentCache.remove(chatId, messageId)
        messagesFilesCache.remove(chatId, messageId)
    }
}
