package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.exposed.onetomany.ExposedKeyValuesRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.libraries.cache.admins.micro_utils.DefaultAdminsCacheAPIRepoImpl
import dev.inmo.tgbotapi.libraries.cache.admins.micro_utils.DynamicAdminsCacheSettingsAPI
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.chat.member.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.exposed.sql.Database

val telegramAdminsSerializationFormat = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        polymorphic(AdministratorChatMember::class) {
            subclass(AdministratorChatMemberImpl::class, AdministratorChatMemberImpl.serializer())
            subclass(OwnerChatMember::class, OwnerChatMember.serializer())
        }
        contextual(AdministratorChatMember::class, PolymorphicSerializer(AdministratorChatMember::class))
    }
}

fun AdminsCacheAPI(
    bot: TelegramBot,
    database: Database,
    scope: CoroutineScope
) : AdminsCacheAPI = DefaultAdminsCacheAPI(
    bot,
    DefaultAdminsCacheAPIRepoImpl(
        ExposedKeyValuesRepo(
            database,
            { long("chatId") },
            { text("member") },
            "AdminsTable"
        ).withMapper<ChatId, AdministratorChatMember, Identifier, String>(
            keyFromToTo = { chatId },
            valueFromToTo = { telegramAdminsSerializationFormat.encodeToString(this) },
            keyToToFrom = { toChatId() },
            valueToToFrom = { telegramAdminsSerializationFormat.decodeFromString(this) }
        ),
        ExposedKeyValueRepo(
            database,
            { long("chatId") },
            { long("datetime") },
            "AdminsUpdatesTimesTable"
        ).withMapper<ChatId, Long, Identifier, Long>(
            keyFromToTo = { chatId },
            valueFromToTo = { this },
            keyToToFrom = { toChatId() },
            valueToToFrom = { this }
        ),
        scope
    ),
    DynamicAdminsCacheSettingsAPI(
        ExposedKeyValueRepo(
            database,
            { long("chatId") },
            { text("settings") },
            "DynamicAdminsCacheSettingsAPI"
        ).withMapper<ChatId, AdminsCacheSettings, Identifier, String>(
            keyFromToTo = { chatId },
            valueFromToTo = { telegramAdminsSerializationFormat.encodeToString(this) },
            keyToToFrom = { toChatId() },
            valueToToFrom = { telegramAdminsSerializationFormat.decodeFromString(this) }
        ),
        scope
    )
)

fun BehaviourContext.AdminsCacheAPI(database: Database) = AdminsCacheAPI(this, database, this)
