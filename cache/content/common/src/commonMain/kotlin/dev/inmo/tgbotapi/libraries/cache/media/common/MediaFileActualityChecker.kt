package dev.inmo.tgbotapi.libraries.cache.media.common

import korlibs.time.DateTime
import korlibs.time.milliseconds
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.DeleteMessage
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.MilliSeconds
import dev.inmo.tgbotapi.types.message.content.MediaContent

fun interface MediaFileActualityChecker {
    suspend fun TelegramBot.isActual(mediaContent: MediaContent): Boolean
    suspend fun TelegramBot.saved(mediaContent: MediaContent) {}

    class Default(
        private val checkingChatId: IdChatIdentifier
    ) : MediaFileActualityChecker {
        override suspend fun TelegramBot.isActual(mediaContent: MediaContent): Boolean {
            return runCatching {
                execute(mediaContent.createResend(checkingChatId)).also { sentMessage ->
                    execute(DeleteMessage(sentMessage.chat.id, sentMessage.messageId))
                }
            }.isSuccess
        }
    }

    class WithDelay(
        private val underhoodChecker: MediaFileActualityChecker,
        private val checkingDelay: MilliSeconds = 24 * 60 * 60 * 1000L // one day
    ) : MediaFileActualityChecker {
        private val fileIdChecksMap = mutableMapOf<FileId, DateTime>()
        private val checkingDelayTimeSpan = checkingDelay.milliseconds

        override suspend fun TelegramBot.isActual(mediaContent: MediaContent): Boolean {
            val now = DateTime.now()
            val lastCheck = fileIdChecksMap[mediaContent.media.fileId]
            return if (lastCheck == null || now - lastCheck > checkingDelayTimeSpan) {
                with(underhoodChecker) {
                    isActual(mediaContent)
                }.also {
                    if (it) {
                        fileIdChecksMap[mediaContent.media.fileId] = now
                    }
                }
            } else {
                true
            }
        }

        override suspend fun TelegramBot.saved(mediaContent: MediaContent) {
            fileIdChecksMap[mediaContent.media.fileId] = DateTime.now()
        }
    }
}
