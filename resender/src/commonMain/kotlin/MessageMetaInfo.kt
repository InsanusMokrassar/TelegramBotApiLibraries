package dev.inmo.tgbotapi.libraries.resender

import dev.inmo.tgbotapi.types.FullChatIdentifierSerializer
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyMediaGroupMessage
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import kotlinx.serialization.Serializable

@Serializable
data class MessageMetaInfo(
    @Serializable(FullChatIdentifierSerializer::class)
    val chatId: IdChatIdentifier,
    val messageId: MessageId,
    val group: String? = null
) {
    val metaInfo: Message.MetaInfo
        get() = Message.MetaInfo(chatId, messageId)
}

fun Message.asMessageMetaInfos(): List<MessageMetaInfo> {
    return if (this is ContentMessage<*>) {
        (content as? MediaGroupContent<*>) ?.group ?.map {
            MessageMetaInfo(it.sourceMessage.chat.id, it.sourceMessage.messageId, it.sourceMessage.mediaGroupId)
        }
    } else {
        null
    } ?: listOf(MessageMetaInfo(chat.id, messageId, (this as? PossiblyMediaGroupMessage<*>) ?.mediaGroupId))
}

operator fun MessageMetaInfo.Companion.invoke(
    message: Message
) = MessageMetaInfo(message.chat.id, message.messageId, (message as? PossiblyMediaGroupMessage<*>) ?.mediaGroupId)
