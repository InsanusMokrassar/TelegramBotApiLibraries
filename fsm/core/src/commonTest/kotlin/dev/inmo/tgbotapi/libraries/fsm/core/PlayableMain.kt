package dev.inmo.tgbotapi.libraries.fsm.core

import kotlin.random.Random

sealed interface TrafficLightState : ImmediateOrNeverState {
    val trafficLightNumber: Int
    override val context: Int
        get() = trafficLightNumber
}
data class GreenCommon(override val trafficLightNumber: Int) : TrafficLightState
data class YellowCommon(override val trafficLightNumber: Int) : TrafficLightState
data class RedCommon(override val trafficLightNumber: Int) : TrafficLightState

suspend fun main() {
    val countOfTrafficLights = 10
    val initialStates = (0 until countOfTrafficLights).map {
        when (Random.nextInt(3)) {
            0 -> GreenCommon(it)
            1 -> YellowCommon(it)
            else -> RedCommon(it)
        }
    }

    val statesHolder = ListBasedStatesHolder(initialStates)
    val machine = StatesMachine(
        statesHolder,

    )
}

