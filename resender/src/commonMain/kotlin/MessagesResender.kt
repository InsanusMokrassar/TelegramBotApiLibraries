package dev.inmo.tgbotapi.libraries.resender

import dev.inmo.micro_utils.common.applyDiff
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.ForwardMessage
import dev.inmo.tgbotapi.requests.ForwardMessages
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
        val messagesWithOrders = messagesInfo.mapIndexed { i, messageInfo -> messageInfo to i }.toMap()
        val ordersWithMessagesGroups = messagesInfo.groupBy { it.group }.flatMap { (group, list) ->
            if (group == null) {
                list.map {
                    messagesWithOrders.getValue(it) to listOf(it)
                }
            } else {
                listOf(messagesWithOrders.getValue(list.first()) to list)
            }
        }.sortedBy { it.first }

        val sent = mutableListOf<MessageMetaInfo>()
        val leftToSend = ordersWithMessagesGroups.map { it.second }.toMutableList()

        return ordersWithMessagesGroups.flatMap { (_, contents) ->
            val sourceMessagesToSentMessages = mutableListOf<Pair<MessageMetaInfo, MessageMetaInfo>>()

            onBetweenMessages(sent.toList(), leftToSend.flatten())

            when {
                contents.size == 1 -> {
                    val messageInfo = contents.first()
                    runCatching {
                        MessageMetaInfo(
                            targetChatId,
                            bot.execute(
                                CopyMessage(
                                    toChatId = targetChatId,
                                    fromChatId = messageInfo.chatId,
                                    messageId = messageInfo.messageId,
                                )
                            )
                        )
                    }.onFailure { _ ->
                        runCatching {
                            bot.execute(
                                ForwardMessage(
                                    toChatId = targetChatId,
                                    fromChatId = messageInfo.chatId,
                                    messageId = messageInfo.messageId
                                )
                            )
                        }.onSuccess {
                            MessageMetaInfo(
                                targetChatId,
                                bot.execute(
                                    CopyMessage(
                                        toChatId = targetChatId,
                                        fromChatId = it.chat.id,
                                        messageId = it.messageId
                                    )
                                )
                            )
                        }
                    }.getOrNull() ?.let {
                        sourceMessagesToSentMessages.add(messageInfo to it)
                    }
                }
                else -> {
                    val resultContents = contents.mapNotNull {
                        it to (
                                bot.execute(
                                    ForwardMessage(
                                        toChatId = cacheChatId,
                                        fromChatId = it.chatId,
                                        messageId = it.messageId
                                    )
                                ) as? ContentMessage<*> ?: return@mapNotNull null)
                    }.mapNotNull { (src, forwardedMessage) ->
                        val forwardedMessageAsMediaPartMessage = forwardedMessage.takeIf {
                            it.content is MediaGroupPartContent
                        } ?.let {
                            it as ContentMessage<MediaGroupPartContent>
                        }
                        src to (forwardedMessageAsMediaPartMessage ?: null.also { _ ->
                            sourceMessagesToSentMessages.add(
                                src to MessageMetaInfo(
                                    targetChatId,
                                    bot.execute(
                                        CopyMessage(
                                            toChatId = targetChatId,
                                            fromChatId = forwardedMessage.chat.id,
                                            messageId = forwardedMessage.messageId
                                        )
                                    )
                                )
                            )
                        } ?: return@mapNotNull null)
                    }

                    resultContents.singleOrNull() ?.also { (src, it) ->
                        sourceMessagesToSentMessages.add(
                            src to MessageMetaInfo(
                                targetChatId,
                                bot.execute(
                                    CopyMessage(
                                        toChatId = targetChatId,
                                        fromChatId = it.chat.id,
                                        messageId = it.messageId
                                    )
                                )
                            )
                        )
                    } ?: resultContents.chunked(mediaCountInMediaGroup.last).forEach {
                        bot.execute(
                            SendMediaGroup<MediaGroupPartContent>(
                                targetChatId,
                                it.map { it.second.content.toMediaGroupMemberTelegramMedia() }
                            )
                        ).content.group.mapIndexed { i, partWrapper ->
                            it.getOrNull(i) ?.let {
                                sourceMessagesToSentMessages.add(
                                    it.first to MessageMetaInfo(
                                        partWrapper.sourceMessage.chat.id,
                                        partWrapper.sourceMessage.messageId,
                                        partWrapper.sourceMessage.mediaGroupId
                                    )
                                )
                            }
                        }
                    }
                }
            }

            leftToSend.takeIf { it.isNotEmpty() } ?.removeAt(0) ?.also {
                sent.addAll(it)
            }
            sourceMessagesToSentMessages.toList()
        }
    }

    suspend fun resend(
        targetChatId: IdChatIdentifier,
        messagesInfo: List<MessageMetaInfo>
    ): List<Pair<MessageMetaInfo, MessageMetaInfo>> = resend(targetChatId, messagesInfo) { _, _ -> }
}
