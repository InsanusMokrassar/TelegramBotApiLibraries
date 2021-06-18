package dev.inmo.tgbotapi.libraries.fsm.core

import dev.inmo.micro_utils.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow

private suspend fun <I : State, O : State> launchStateHandling(
    state: State,
    handlers: List<StateHandlerHolder<out I, out O>>
): O? {
    return handlers.firstOrNull { it.checkHandleable(state) } ?.handleState(
        state
    )
}

class StatesMachine<T : State, I : T, O : T>(
    private val statesManager: StatesManager<T>,
    private val handlers: List<StateHandlerHolder<out I, out O>>
) : StatesHandler<T, O> {
    override suspend fun handleState(state: T): O? {
        return launchStateHandling(state, handlers)
    }

    fun start(scope: CoroutineScope): Job = scope.launchSafelyWithoutExceptions {
        val statePerformer: suspend (T) -> Unit = { state: T ->
            val newState = handleState(state)
            if (newState != null) {
                statesManager.update(state, newState)
            } else {
                statesManager.endChain(state)
            }
        }
        statesManager.onStartChain.subscribeSafelyWithoutExceptions(this) {
            launch { statePerformer(it) }
        }
        statesManager.onChainStateUpdated.subscribeSafelyWithoutExceptions(this) {
            launch { statePerformer(it.second) }
        }

        statesManager.getActiveStates().forEach {
            launch { statePerformer(it) }
        }
    }
}
