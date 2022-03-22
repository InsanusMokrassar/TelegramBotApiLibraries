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
import io.ktor.utils.io.cancel

class DefaultMessageContentCache<T>(
    private val bot: TelegramBot,
    private val simpleMessageContentCache: MessageContentCache,
    private val messagesFilesCache: MessagesFilesCache,
    private val filesRefreshingChatId: ChatId
) : MessageContentCache {
    override suspend fun save(chatId: ChatId, messageId: MessageIdentifier, content: MessageContent): Boolean {
        runCatching {
            if (content is MediaContent) {
                val extendedInfo = bot.execute(
                    GetFile(content.media.fileId)
                )
                val allocator = bot.execute(
                    DownloadFileStream(
                        extendedInfo.filePath
                    )
                )
                messagesFilesCache.set(chatId, messageId, extendedInfo.fileName, allocator)
            }
        }.onFailure {
            return false
        }

        return simpleMessageContentCache.save(
            chatId, messageId, content
        )
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

                simpleMessageContentCache.save(chatId, messageId, newContent.content)
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
