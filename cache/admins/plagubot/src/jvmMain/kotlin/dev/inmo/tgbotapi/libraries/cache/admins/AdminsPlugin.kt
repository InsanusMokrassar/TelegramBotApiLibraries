package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.exposed.onetomany.ExposedKeyValuesRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.libraries.cache.admins.micro_utils.DefaultAdminsCacheAPIRepoImpl
import dev.inmo.tgbotapi.libraries.cache.admins.micro_utils.DynamicAdminsCacheSettingsAPI
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.binds

val Scope.adminsPlugin: AdminsPlugin?
    get() = getOrNull()

val Koin.adminsPlugin: AdminsPlugin?
    get() = getOrNull()

@Serializable
class AdminsPlugin : Plugin {
    @Transient
    private val globalAdminsCacheAPI = MutableStateFlow<AdminsCacheAPI?>(null)
    @Transient
    private val databaseToAdminsCacheAPI = mutableMapOf<Database, MutableStateFlow<AdminsCacheAPI?>>()
    private val mutex = Mutex()

    @Deprecated("Will be removed soon due to its redundancy")
    suspend fun adminsAPI(database: Database): AdminsCacheAPI {
        val flow = mutex.withLock {
            databaseToAdminsCacheAPI.getOrPut(database){ MutableStateFlow(null) }
        }
        return flow.filterNotNull().first()
    }

    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { this@AdminsPlugin }
        val scopeQualifier = named("admins plugin scope")
        single(scopeQualifier) { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
        single<DefaultAdminsCacheAPIRepo> {
            DefaultAdminsCacheAPIRepoImpl(
                ExposedKeyValuesRepo(
                    database,
                    { long("chatId") },
                    { text("member") },
                    "AdminsTable"
                ).withMapper<IdChatIdentifier, AdministratorChatMember, Identifier, String>(
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
                ).withMapper<IdChatIdentifier, Long, Identifier, Long>(
                    keyFromToTo = { chatId },
                    valueFromToTo = { this },
                    keyToToFrom = { toChatId() },
                    valueToToFrom = { this }
                ),
                get(scopeQualifier)
            )
        }
        single<AdminsCacheSettingsAPI> {
            DynamicAdminsCacheSettingsAPI(
                ExposedKeyValueRepo(
                    database,
                    { long("chatId") },
                    { text("settings") },
                    "DynamicAdminsCacheSettingsAPI"
                ).withMapper<IdChatIdentifier, AdminsCacheSettings, Identifier, String>(
                    keyFromToTo = { chatId },
                    valueFromToTo = { telegramAdminsSerializationFormat.encodeToString(this) },
                    keyToToFrom = { toChatId() },
                    valueToToFrom = { telegramAdminsSerializationFormat.decodeFromString(this) }
                ),
                get(scopeQualifier)
            )
        }
        single { DefaultAdminsCacheAPI(get(), get(), get()) } binds arrayOf(
            AdminsCacheAPI::class
        )
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        with(koin) {
            activateAdminsChangesListening(
                get()
            )
        }
    }
}
