package dev.inmo.tgbotapi.libraries.cache.admins.micro_utils

import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.repos.*
import dev.inmo.tgbotapi.libraries.cache.admins.*
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class DynamicAdminsCacheSettingsAPI(
    private val repo: KeyValueRepo<ChatId, AdminsCacheSettings>,
    private val scope: CoroutineScope
) : AdminsCacheSettingsAPI, MutableAdminsCacheSettingsAPI {
    override val chatSettingsUpdatedFlow: SharedFlow<Pair<ChatId, AdminsCacheSettings>>
        get() = repo.onNewValue.shareIn(scope, SharingStarted.Eagerly)

    override suspend fun setChatSettings(chatId: ChatId, settings: AdminsCacheSettings) {
        repo.set(chatId, settings)
    }

    override suspend fun getChatSettings(chatId: ChatId): AdminsCacheSettings {
        val settings = repo.get(chatId)
        return if (settings == null) {
            val newSettings = AdminsCacheSettings()
            setChatSettings(chatId, newSettings)
            newSettings
        } else {
            settings
        }
    }
}