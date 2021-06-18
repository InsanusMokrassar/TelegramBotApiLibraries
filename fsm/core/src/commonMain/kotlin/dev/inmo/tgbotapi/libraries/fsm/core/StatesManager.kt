package dev.inmo.tgbotapi.libraries.fsm.core

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface StatesManager<T : State> {
    val onChainStateUpdated: Flow<Pair<T, T>>
    val onStartChain: Flow<T>
    val onEndChain: Flow<T>


    /**
     * Must set current set using [State.context]
     */
    suspend fun update(old: T, new: T)

    /**
     * Starts chain with [state] as first [State]. May returns false in case of [State.context] of [state] is already
     * busy by the other [State]
     */
    suspend fun startChain(state: T)

    /**
     * Ends chain with context from [state]. In case when [State.context] of [state] is absent, [state] should be just
     * ignored
     */
    suspend fun endChain(state: T)

    suspend fun getActiveStates(): List<T>
}

/**
 * @param onContextsConflictResolver Receive old [State], new one and the state currently placed on new [State.context]
 * key. In case when this callback will returns true, the state placed on [State.context] of new will be replaced by
 * new state by using [endChain] with that state
 */
class InMemoryStatesManager<T : State>(
    private val onContextsConflictResolver: suspend (old: T, new: T, currentNew: T) -> Boolean = { _, _, _ -> true }
) : StatesManager<T> {
    private val _onChainStateUpdated = MutableSharedFlow<Pair<T, T>>(0)
    override val onChainStateUpdated: Flow<Pair<T, T>> = _onChainStateUpdated.asSharedFlow()
    private val _onStartChain = MutableSharedFlow<T>(0)
    override val onStartChain: Flow<T> = _onStartChain.asSharedFlow()
    private val _onEndChain = MutableSharedFlow<T>(0)
    override val onEndChain: Flow<T> = _onEndChain.asSharedFlow()

    private val contextsToStates = mutableMapOf<Any, T>()
    private val mapMutex = Mutex()

    override suspend fun update(old: T, new: T) = mapMutex.withLock {
        when {
            contextsToStates[old.context] != old -> return@withLock
            old.context == new.context || !contextsToStates.containsKey(new.context) -> {
                contextsToStates[old.context] = new
                _onChainStateUpdated.emit(old to new)
            }
            else -> {
                val stateOnNewOneContext = contextsToStates.getValue(new.context)
                if (onContextsConflictResolver(old, new, stateOnNewOneContext)) {
                    endChainWithoutLock(stateOnNewOneContext)
                    contextsToStates.remove(old.context)
                    contextsToStates[new.context] = new
                    _onChainStateUpdated.emit(old to new)
                }
            }
        }
    }

    override suspend fun startChain(state: T) = mapMutex.withLock {
        if (!contextsToStates.containsKey(state.context)) {
            contextsToStates[state.context] = state
            _onStartChain.emit(state)
        }
    }

    private suspend fun endChainWithoutLock(state: T) {
        if (contextsToStates[state.context] == state) {
            contextsToStates.remove(state.context)
            _onEndChain.emit(state)
        }
    }

    override suspend fun endChain(state: T) {
        mapMutex.withLock {
            endChainWithoutLock(state)
        }
    }

    override suspend fun getActiveStates(): List<T> = contextsToStates.values.toList()

}
