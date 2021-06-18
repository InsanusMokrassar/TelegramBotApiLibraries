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
    private val statesRepo: StatesRepo<T>,
    private val statesQuotaManager: StatesQuotaManager,
    private val handlers: List<StateHandlerHolder<out I, out O>>
) : StatesHandler<T, O> {
    override suspend fun handleState(state: T): O? {
        return statesQuotaManager.doOnQuota(
            state
        ) {
            launchStateHandling(state, handlers)
        }
    }

    fun start(scope: CoroutineScope): Job = scope.launchSafelyWithoutExceptions {
        val statesFlow = statesRepo.loadStates().asFlow() + statesRepo.onNewState
        statesFlow.subscribeSafelyWithoutExceptions(this) {
            val newState = handleState(it)
            newState ?.also { statesRepo.saveState(newState) }
            statesRepo.removeState(it)
        }
    }
}
