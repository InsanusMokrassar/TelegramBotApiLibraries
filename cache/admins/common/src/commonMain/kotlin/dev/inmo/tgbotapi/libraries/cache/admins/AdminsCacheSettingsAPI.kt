package dev.inmo.tgbotapi.libraries.cache.admins

import com.soywiz.klock.minutes
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.Seconds
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable

@Serializable
data class AdminsCacheSettings(
    val refreshSeconds: Seconds = 10.minutes.seconds.toInt(),
    /**
     * In case this setting set up to true, will request admins list just from time to time instead of refreshing on
     * requests
     */
    val disableRequestsRefreshMode: Boolean = false
) {
    val refreshOnCacheCalls: Boolean
        get() = !disableRequestsRefreshMode
    @Deprecated("Renamed", ReplaceWith("refreshOnCacheCalls"))
    val refreshOnRequests: Boolean
        get() = refreshOnCacheCalls
}

interface AdminsCacheSettingsAPI {
    suspend fun getChatSettings(chatId: ChatId): AdminsCacheSettings?
}

interface MutableAdminsCacheSettingsAPI : AdminsCacheSettingsAPI {
    val chatSettingsUpdatedFlow: SharedFlow<Pair<ChatId, AdminsCacheSettings>>

    suspend fun setChatSettings(chatId: ChatId, settings: AdminsCacheSettings)
}

fun AdminsCacheSettingsAPI.asMutable(): MutableAdminsCacheSettingsAPI? = this as? MutableAdminsCacheSettingsAPI

@Serializable
class StaticAdminsCacheSettingsAPI(
    private val settings: Map<ChatId, AdminsCacheSettings>
) : AdminsCacheSettingsAPI {
    override suspend fun getChatSettings(chatId: ChatId): AdminsCacheSettings? = settings[chatId]
}


