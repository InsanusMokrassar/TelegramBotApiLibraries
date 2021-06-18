package dev.inmo.tgbotapi.libraries.fsm.core

import kotlin.reflect.KClass

class StateHandlerHolder<I : State, O : State>(
    private val inputKlass: KClass<I>,
    private val strict: Boolean = false,
    private val delegateTo: StatesHandler<I, O>
) : StatesHandler<State, O> {
    fun checkHandleable(state: State) = state::class == inputKlass || (!strict && inputKlass.isInstance(state))

    override suspend fun handleState(state: State): O? {
        return delegateTo.handleState(state as I)
    }
}
