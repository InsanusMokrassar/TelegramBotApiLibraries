package dev.inmo.tgbotapi.libraries.fsm.core

fun interface StatesHandler<I : State, O : State> {
    suspend fun handleState(state: I): O?
}
