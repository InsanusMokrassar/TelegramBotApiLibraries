package dev.inmo.tgbotapi.libraries.fsm.core

import kotlinx.coroutines.flow.Flow

interface StatesRepo<T : State> {
    val onNewState: Flow<T>
    val onStateRemoved: Flow<T>

    suspend fun saveState(state: T)
    suspend fun removeState(state: T)
    suspend fun loadStates(): List<T>
}
