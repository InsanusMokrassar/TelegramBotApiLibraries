package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.exposed.onetomany.ExposedOneToManyKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.libraries.cache.admins.micro_utils.DefaultAdminsCacheAPIRepo
import dev.inmo.tgbotapi.libraries.cache.admins.micro_utils.DynamicAdminsCacheSettingsAPI
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

private val serializationFormat = Cbor

fun AdminsCacheAPI(
    bot: TelegramBot,
    database: Database,
    scope: CoroutineScope,
    repo: dev.inmo.tgbotapi.libraries.cache.admins.DefaultAdminsCacheAPIRepo = DefaultAdminsCacheAPIRepo(
        ExposedOneToManyKeyValueRepo(
            database,
            { long("chatId") },
            { blob("member") },
            "AdminsTable"
        ).withMapper(
            keyFromToTo = { chatId },
            valueFromToTo = { ExposedBlob(serializationFormat.encodeToByteArray(this)) },
            keyToToFrom = { toChatId() },
            valueToToFrom = { serializationFormat.decodeFromByteArray(bytes) }
        ),
        ExposedKeyValueRepo(
            database,
            { long("chatId") },
            { long("datetime") },
            "AdminsUpdatesTimesTable"
        ).withMapper(
            keyFromToTo = { chatId },
            keyToToFrom = { toChatId() }
        ),
        scope
    ),
    settingsAPI: AdminsCacheSettingsAPI = DynamicAdminsCacheSettingsAPI(
        ExposedKeyValueRepo(
            database,
            { long("chatId") },
            { blob("settings") },
            "DynamicAdminsCacheSettingsAPI"
        ).withMapper(
            keyFromToTo = { chatId },
            valueFromToTo = { ExposedBlob(serializationFormat.encodeToByteArray(this)) },
            keyToToFrom = { toChatId() },
            valueToToFrom = { serializationFormat.decodeFromByteArray(bytes) }
        ),
        scope
    )
) : AdminsCacheAPI = DefaultAdminsCacheAPI(
    bot,
    repo,
    settingsAPI
)
