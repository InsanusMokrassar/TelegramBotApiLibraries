package dev.inmo.tgbotapi.libraries.resender

import dev.inmo.micro_utils.common.applyDiff
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.ForwardMessage
import dev.inmo.tgbotapi.requests.send.CopyMessage
import dev.inmo.tgbotapi.requests.send.CopyMessages
import dev.inmo.tgbotapi.requests.send.media.SendMediaGroup
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.mediaCountInMediaGroup
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent

class MessagesResender(
    private val bot: TelegramBot,
    private val cacheChatId: ChatIdentifier
) {
    suspend fun resend(
        targetChatId: IdChatIdentifier,
        messagesInfo: List<MessageMetaInfo>,
        onBetweenMessages: suspend (sent: List<MessageMetaInfo>, toBeSent: List<MessageMetaInfo>) -> Unit
    ): List<Pair<MessageMetaInfo, MessageMetaInfo>> {
        val currentGroup = mutableListOf<MessageMetaInfo>()
        suspend fun makeCopy(): List<Pair<MessageMetaInfo, MessageMetaInfo>> {
            currentGroup.sortBy { it.messageId }
            while (currentGroup.isNotEmpty()) {
                return runCatching {
                    bot.execute(
                        CopyMessages(
                            toChatId = targetChatId,
                            fromChatId = currentGroup.firstOrNull() ?.chatId ?: return emptyList(),
                            messageIds = currentGroup.map { it.messageId }
                        )
                    ).mapIndexed { i, newMessageId ->
                        currentGroup[i] to MessageMetaInfo(targetChatId, newMessageId)
                    }.also {
                        currentGroup.clear()
                    }
                }.getOrElse {
                    currentGroup.applyDiff(
                        currentGroup.filter {
                            runCatching {
                                bot.execute(
                                    ForwardMessage(
                                        toChatId = cacheChatId,
                                        fromChatId = it.chatId,
                                        messageId = it.messageId
                                    )
                                )
                            }.isSuccess
                        }
                    )
                    null
                } ?: continue
            }
            return emptyList()
        }

        val copied = mutableListOf<Pair<MessageMetaInfo, MessageMetaInfo>>()
        for (content in messagesInfo) {
            when {
                currentGroup.isEmpty() ||
                currentGroup.first().chatId == content.chatId -> currentGroup.add(content)
                else -> {
                    onBetweenMessages(copied.map { it.first }, currentGroup.toList())
                    copied.addAll(makeCopy())
                }
            }
        }
        if (currentGroup.isNotEmpty()) {
            onBetweenMessages(copied.map { it.first }, currentGroup.toList())
            copied.addAll(makeCopy())
        }
        return copied.toList()
    }

    suspend fun resend(
        targetChatId: IdChatIdentifier,
        messagesInfo: List<MessageMetaInfo>
    ): List<Pair<MessageMetaInfo, MessageMetaInfo>> = resend(targetChatId, messagesInfo) { _, _ -> }
}
