package nl.pindab0ter.eggbot.simulation.rework

import org.joda.time.Duration
import java.math.BigDecimal

object ReworkedSoloSimulation {
    private val ONE_MINUTE: Duration = Duration.standardMinutes(1)

    tailrec fun catchUp(state: FarmState): FarmState = when (state.timeSinceLastUpdate) {
        Duration.ZERO -> state
        else -> catchUp(
            state.copy(
                timeSinceLastUpdate = state.timeSinceLastUpdate - ONE_MINUTE,
                eggs = state.eggs + state.chickens
            )
        )
    }

    tailrec fun simulate(state: FarmState): FarmState = when (state.timeRemaining) {
        Duration.ZERO -> state
        else -> simulate(
            state.copy(
                timeRemaining = state.timeRemaining - ONE_MINUTE,
                eggs = state.eggs + state.chickens
            )
        )
    }
}

data class FarmState(
    val timeSinceLastUpdate: Duration,
    val timeRemaining: Duration,
    val eggs: BigDecimal = BigDecimal.ZERO,
    val chickens: BigDecimal
)

fun main() {
    val catchUp = true
    val state = FarmState(
        timeSinceLastUpdate = Duration.standardHours(18),
        timeRemaining = Duration.standardDays(3),
        chickens = BigDecimal.ONE
    )

    val caughtUpState = if (catchUp) ReworkedSoloSimulation.catchUp(state) else state
    val simulatedState = ReworkedSoloSimulation.simulate(caughtUpState)
    println(simulatedState)
}