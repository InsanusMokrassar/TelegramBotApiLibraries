import dev.inmo.tgbotapi.libraries.fsm.core.*
import dev.inmo.tgbotapi.libraries.fsm.core.dsl.buildFSM
import dev.inmo.tgbotapi.libraries.fsm.core.dsl.strictlyOn
import kotlinx.coroutines.*
import kotlin.random.Random
import kotlin.test.Test

sealed interface TrafficLightState : ImmediateOrNeverState {
    val trafficLightNumber: Int
    override val context: Int
        get() = trafficLightNumber
}
data class GreenCommon(override val trafficLightNumber: Int) : TrafficLightState
data class YellowCommon(override val trafficLightNumber: Int) : TrafficLightState
data class RedCommon(override val trafficLightNumber: Int) : TrafficLightState

class PlayableMain {
    fun test() {
        runBlocking {
            val countOfTrafficLights = 10
            val initialStates = (0 until countOfTrafficLights).map {
                when (0/*Random.nextInt(3)*/) {
                    0 -> GreenCommon(it)
                    1 -> YellowCommon(it)
                    else -> RedCommon(it)
                }
            }

            val statesManager = InMemoryStatesManager()

            val machine = buildFSM {
                strictlyOn<GreenCommon> {
                    delay(1000L)
                    YellowCommon(it.context).also(::println)
                }
                strictlyOn<YellowCommon> {
                    delay(1000L)
                    RedCommon(it.context).also(::println)
                }
                strictlyOn<RedCommon> {
                    delay(1000L)
                    GreenCommon(it.context).also(::println)
                }
                this.statesManager = statesManager
            }

            initialStates.forEach { machine.startChain(it) }

            val scope = CoroutineScope(Dispatchers.Default)
            machine.start(scope).join()

        }
    }
}
