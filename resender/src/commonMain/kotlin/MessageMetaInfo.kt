package dev.inmo.tgbotapi.libraries.resender

import dev.inmo.tgbotapi.types.FullChatIdentifierSerializer
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyMediaGroupMessage
import kotlinx.serialization.Serializable

@Serializable
data class MessageMetaInfo(
    @Serializable(FullChatIdentifierSerializer::class)
    val chatId: IdChatIdentifier,
    val messageId: MessageId,
    val group: String? = null
)

operator fun MessageMetaInfo.Companion.invoke(
    message: Message
) = MessageMetaInfo(message.chat.id, message.messageId, (message as? PossiblyMediaGroupMessage<*>) ?.mediaGroupId)
