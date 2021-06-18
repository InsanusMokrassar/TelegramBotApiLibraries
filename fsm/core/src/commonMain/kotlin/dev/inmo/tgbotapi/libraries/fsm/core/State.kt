package dev.inmo.tgbotapi.libraries.fsm.core

sealed interface State {
    val context: Any
}

/**
 * Use this state as parent of your state in case you want to avoid saving of this state in queue for [context] if this
 * queue is not empty
 */
interface ImmediateOrNeverState : State

/**
 * Use this state as parent of your state in case you want to keep saving of this state in queue for [context] if this
 * queue is not empty
 */
interface QueueableState : State
