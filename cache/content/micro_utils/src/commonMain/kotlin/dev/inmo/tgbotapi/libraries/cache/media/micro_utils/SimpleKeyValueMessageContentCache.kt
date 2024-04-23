package dev.inmo.tgbotapi.libraries.cache.media.micro_utils

import dev.inmo.micro_utils.repos.*
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.tgbotapi.libraries.cache.media.common.MessagesSimpleCache
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.message.content.MessageContent
import kotlinx.serialization.*
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.js.JsName
import kotlin.jvm.JvmName

class SimpleKeyValueMessageContentCache<K>(
    private val keyValueRepo: KeyValueRepo<K, MessageContent>
) : MessagesSimpleCache<K> {
    override suspend fun set(k: K, content: MessageContent) {
        keyValueRepo.set(k, content)
    }

    override suspend fun update(k: K, content: MessageContent): Boolean {
        return keyValueRepo.runCatching {
            if (contains(k)) {
                keyValueRepo.set(k, content)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    override suspend fun get(k: K): MessageContent? {
        return keyValueRepo.get(k)
    }

    override suspend fun contains(k: K): Boolean {
        return keyValueRepo.contains(k)
    }

    override suspend fun remove(k: K) {
        keyValueRepo.unset(k)
    }
}

val chatIdToMessageIdentifierSerializer = PairSerializer(
    ChatId.serializer(),
    MessageId.serializer()
)

val messageContentSerializer = PolymorphicSerializer<MessageContent>(MessageContent::class)

inline fun <K> KeyValueRepo<K, MessageContent>.asMessageContentCache() = SimpleKeyValueMessageContentCache(this)

inline fun KeyValueRepo<String, String>.asMessageContentCache(
    serialFormatCreator: (SerializersModule) -> StringFormat = { Json { serializersModule = it } }
): StandardKeyValueRepo<Pair<ChatId, MessageId>, MessageContent> {
    val serialFormat = serialFormatCreator(MessageContent.serializationModule())
    return withMapper<Pair<ChatId, MessageId>, MessageContent, String, String>(
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
): StandardKeyValueRepo<Pair<ChatId, MessageId>, MessageContent> {
    val serialFormat = serialFormatCreator(MessageContent.serializationModule())
    return withMapper<Pair<ChatId, MessageId>, MessageContent, String, String>(
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
): StandardKeyValueRepo<Pair<ChatId, MessageId>, MessageContent> {
    val serialFormat = serialFormatCreator(MessageContent.serializationModule())
    return withMapper<Pair<ChatId, MessageId>, MessageContent, ByteArray, ByteArray>(
        { serialFormat.encodeToByteArray(chatIdToMessageIdentifierSerializer, this) },
        { serialFormat.encodeToByteArray(messageContentSerializer, this) },
        { serialFormat.decodeFromByteArray(chatIdToMessageIdentifierSerializer, this) },
        { serialFormat.decodeFromByteArray(messageContentSerializer, this) },
    )
}
