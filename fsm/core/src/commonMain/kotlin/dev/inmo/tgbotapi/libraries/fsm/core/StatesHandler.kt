package dev.inmo.tgbotapi.libraries.fsm.core

fun interface StatesHandler<I : State> {
    suspend fun StatesMachine.handleState(state: I): State?
}
