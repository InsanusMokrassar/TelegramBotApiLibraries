package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.scope.Scope

val Scope.adminsPlugin: AdminsPlugin?
    get() = getOrNull()

val Koin.adminsPlugin: AdminsPlugin?
    get() = getOrNull()

@Serializable
class AdminsPlugin(
    private val chatsSettings: Map<ChatId, AdminsCacheSettings>? = null
) : Plugin {
    @Transient
    private val globalAdminsCacheAPI = MutableStateFlow<AdminsCacheAPI?>(null)
    @Transient
    private val databaseToAdminsCacheAPI = mutableMapOf<Database, MutableStateFlow<AdminsCacheAPI?>>()
    private val mutex = Mutex()

    suspend fun adminsAPI(database: Database): AdminsCacheAPI {
        return when (chatsSettings) {
            null -> {
                val flow = mutex.withLock {
                    databaseToAdminsCacheAPI.getOrPut(database){ MutableStateFlow(null) }
                }
                flow.first { it != null }!!
            }
            else -> globalAdminsCacheAPI.first { it != null }!!
        }
    }

    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { this@AdminsPlugin }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        with(koin) {
            when (chatsSettings) {
                null -> {
                    mutex.withLock {
                        val flow = databaseToAdminsCacheAPI.getOrPut(koin.get()){ MutableStateFlow(null) }
                        if (flow.value == null) {
                            flow.value = AdminsCacheAPI(koin.get())
                        }
                    }
                }
                else -> mutex.withLock {
                    globalAdminsCacheAPI.value = AdminsCacheAPI(koin.get())
                }
            }
        }
    }
}
