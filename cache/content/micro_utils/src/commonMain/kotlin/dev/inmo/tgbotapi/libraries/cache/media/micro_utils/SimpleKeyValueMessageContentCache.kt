package dev.inmo.tgbotapi.libraries.cache.media.micro_utils

import dev.inmo.micro_utils.repos.*
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.tgbotapi.libraries.cache.media.common.MessageContentCache
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent
import kotlinx.serialization.*
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.js.JsName
import kotlin.jvm.JvmName

class SimpleKeyValueMessageContentCache(
    private val keyValueRepo: KeyValueRepo<Pair<ChatId, MessageIdentifier>, MessageContent>
) : MessageContentCache {
    override suspend fun save(chatId: ChatId, messageId: MessageIdentifier, content: MessageContent): Boolean {
        return keyValueRepo.runCatching {
            set(chatId to messageId, content)
        }.isSuccess
    }

    override suspend fun get(chatId: ChatId, messageId: MessageIdentifier): MessageContent? {
        return keyValueRepo.get(chatId to messageId)
    }

    override suspend fun contains(chatId: ChatId, messageId: MessageIdentifier): Boolean {
        return keyValueRepo.contains(chatId to messageId)
    }

    override suspend fun remove(chatId: ChatId, messageId: MessageIdentifier) {
        keyValueRepo.unset(chatId to messageId)
    }
}

val chatIdToMessageIdentifierSerializer = PairSerializer(
    ChatId.serializer(),
    MessageIdentifier.serializer()
)

val messageContentSerializer = PolymorphicSerializer<MessageContent>(MessageContent::class)

inline fun KeyValueRepo<String, String>.asMessageContentCache(
    serialFormatCreator: (SerializersModule) -> StringFormat = { Json { serializersModule = it } }
): StandardKeyValueRepo<Pair<ChatId, MessageIdentifier>, MessageContent> {
    val serialFormat = serialFormatCreator(MessageContent.serializationModule())
    return withMapper<Pair<ChatId, MessageIdentifier>, MessageContent, String, String>(
        { serialFormat.encodeToString(chatIdToMessageIdentifierSerializer, this) },
        { serialFormat.encodeToString(messageContentSerializer, this) },
        { serialFormat.decodeFromString(chatIdToMessageIdentifierSerializer, this) },
        { serialFormat.decodeFromString(messageContentSerializer, this) },
    )
}

@JvmName("stringsKeyValueAsHexMessageContentCache")
@JsName("stringsKeyValueAsHexMessageContentCache")
inline fun KeyValueRepo<String, String>.asMessageContentCache(
    serialFormatCreator: (SerializersModule) -> BinaryFormat
): StandardKeyValueRepo<Pair<ChatId, MessageIdentifier>, MessageContent> {
    val serialFormat = serialFormatCreator(MessageContent.serializationModule())
    return withMapper<Pair<ChatId, MessageIdentifier>, MessageContent, String, String>(
        { serialFormat.encodeToHexString(chatIdToMessageIdentifierSerializer, this) },
        { serialFormat.encodeToHexString(messageContentSerializer, this) },
        { serialFormat.decodeFromHexString(chatIdToMessageIdentifierSerializer, this) },
        { serialFormat.decodeFromHexString(messageContentSerializer, this) },
    )
}

@JvmName("bytesKeyValueAsMessageContentCache")
@JsName("bytesKeyValueAsMessageContentCache")
inline fun KeyValueRepo<ByteArray, ByteArray>.asMessageContentCache(
    serialFormatCreator: (SerializersModule) -> BinaryFormat
): StandardKeyValueRepo<Pair<ChatId, MessageIdentifier>, MessageContent> {
    val serialFormat = serialFormatCreator(MessageContent.serializationModule())
    return withMapper<Pair<ChatId, MessageIdentifier>, MessageContent, ByteArray, ByteArray>(
        { serialFormat.encodeToByteArray(chatIdToMessageIdentifierSerializer, this) },
        { serialFormat.encodeToByteArray(messageContentSerializer, this) },
        { serialFormat.decodeFromByteArray(chatIdToMessageIdentifierSerializer, this) },
        { serialFormat.decodeFromByteArray(messageContentSerializer, this) },
    )
}