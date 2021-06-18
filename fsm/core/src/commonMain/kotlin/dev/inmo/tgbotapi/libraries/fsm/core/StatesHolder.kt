package dev.inmo.tgbotapi.libraries.fsm.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

interface StatesHolder<T : State> {
    val onNewState: Flow<T>
    val onStateRemoved: Flow<T>

    /**
     * Must always save [state] inside of [this] [StatesHolder] AND send event in [onNewState]
     */
    suspend fun saveState(state: T)

    /**
     * Must always remove [state] in case it is stored AND send event in [onStateRemoved] if it was removed
     */
    suspend fun removeState(state: T)

    /**
     * Must returns currently stored states
     */
    suspend fun loadStates(): List<T>
}

class ListBasedStatesHolder<T : State>(
    base: List<T> = emptyList()
) : StatesHolder<T> {
    private val data: MutableList<T> = base.toMutableList()
    private val _onNewState = MutableSharedFlow<T>(0, onBufferOverflow = BufferOverflow.SUSPEND)
    override val onNewState: Flow<T> = _onNewState.asSharedFlow()
    private val _onStateRemoved = MutableSharedFlow<T>(0, onBufferOverflow = BufferOverflow.SUSPEND)
    override val onStateRemoved: Flow<T> = _onStateRemoved.asSharedFlow()

    override suspend fun saveState(state: T) {
        data.add(state)
        _onNewState.emit(state)
    }

    override suspend fun removeState(state: T) {
        if (data.remove(state)) {
            _onStateRemoved.emit(state)
        }
    }

    override suspend fun loadStates(): List<T> = data.toList()

}
