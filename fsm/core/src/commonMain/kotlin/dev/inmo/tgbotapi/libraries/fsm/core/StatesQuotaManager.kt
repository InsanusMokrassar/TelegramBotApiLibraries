package dev.inmo.tgbotapi.libraries.fsm.core

interface StatesQuotaManager {
    suspend fun <T : State, O : State> doOnQuota(state: T, block: suspend (T) -> O?): O?
}
