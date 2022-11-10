package dev.inmo.tgbotapi.libraries.cache.admins.micro_utils

import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.repos.*
import dev.inmo.tgbotapi.libraries.cache.admins.*
import dev.inmo.tgbotapi.types.IdChatIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class DynamicAdminsCacheSettingsAPI(
    private val repo: KeyValueRepo<IdChatIdentifier, AdminsCacheSettings>,
    private val scope: CoroutineScope
) : AdminsCacheSettingsAPI, MutableAdminsCacheSettingsAPI {
    override val chatSettingsUpdatedFlow: SharedFlow<Pair<IdChatIdentifier, AdminsCacheSettings>>
        get() = repo.onNewValue.shareIn(scope, SharingStarted.Eagerly)

    override suspend fun setChatSettings(chatId: IdChatIdentifier, settings: AdminsCacheSettings) {
        repo.set(chatId, settings)
    }

    override suspend fun getChatSettings(chatId: IdChatIdentifier): AdminsCacheSettings {
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
