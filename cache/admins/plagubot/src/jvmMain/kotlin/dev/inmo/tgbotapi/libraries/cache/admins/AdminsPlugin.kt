package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.Database

val Map<String, Any>.adminsPlugin: AdminsPlugin?
    get() = get("admins") as? AdminsPlugin

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

    override suspend fun BehaviourContext.invoke(database: Database, params: Map<String, Any>) {
        when (chatsSettings) {
            null -> {
                mutex.withLock {
                    val flow = databaseToAdminsCacheAPI.getOrPut(database){ MutableStateFlow(null) }
                    if (flow.value == null) {
                        flow.value = AdminsCacheAPI(database)
                    }
                }
            }
            else -> mutex.withLock {
                globalAdminsCacheAPI.value = AdminsCacheAPI(database)
            }
        }
    }
}
