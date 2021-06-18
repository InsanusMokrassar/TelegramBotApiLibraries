package dev.inmo.tgbotapi.libraries.fsm.core

import kotlinx.coroutines.sync.Mutex

interface StatesQuotaManager {
    suspend fun <T : State, O : State> doOnQuota(state: T, block: suspend (T) -> O?): O?

    suspend fun transitQuota(from: State, to: State?)
}

class InMemoryStatesQuotaManager : StatesQuotaManager {
    private val currentContextsAndStates = mutableMapOf<Any, State>()

    private val mutex = Mutex()

    override suspend fun <T : State, O : State> doOnQuota(state: T, block: suspend (T) -> O?): O? {

    }

    override suspend fun transitQuota(state: State) {
        TODO("Not yet implemented")
    }
}
