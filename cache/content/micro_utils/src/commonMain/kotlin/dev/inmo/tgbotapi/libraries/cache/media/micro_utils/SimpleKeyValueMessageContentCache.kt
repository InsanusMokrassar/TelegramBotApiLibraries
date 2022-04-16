package dev.inmo.tgbotapi.libraries.cache.media.micro_utils

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.repos.*
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.tgbotapi.libraries.cache.media.common.MessagesSimpleCache
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

class SimpleKeyValueMessageContentCache<K>(
    private val keyValueRepo: KeyValueRepo<K, MessageContent>,
    private val keyGenerator: () -> K
) : MessagesSimpleCache<K> {
    override suspend fun add(content: MessageContent): K {
        val key = keyGenerator()
        keyValueRepo.set(key, content)

        return key
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

    companion object {
        operator fun invoke(
            keyValueRepo: KeyValueRepo<String, MessageContent>
        ) = SimpleKeyValueMessageContentCache(keyValueRepo) { uuid4().toString() }
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
