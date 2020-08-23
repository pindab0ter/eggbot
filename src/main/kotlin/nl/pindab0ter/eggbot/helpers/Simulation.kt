package nl.pindab0ter.eggbot.helpers

import nl.pindab0ter.eggbot.model.simulation.new.Constants
import nl.pindab0ter.eggbot.model.simulation.new.FarmState
import nl.pindab0ter.eggbot.model.simulation.new.Hab
import org.joda.time.Duration
import java.math.BigDecimal

/** Base egg laying rate per chicken per minute.
 *
 * A chicken lays 1/30 of an egg per second, so 2 per minute */
private val EGG_LAYING_BASE_RATE = BigDecimal(2)

tailrec fun catchUp(
    state: FarmState,
    constants: Constants,
    catchupTimeLeft: Duration,
): FarmState = when {
    catchupTimeLeft <= Duration.ZERO -> state
    else -> catchUp(
        state = advanceOneMinute(state, constants),
        constants = constants,
        catchupTimeLeft = catchupTimeLeft - ONE_MINUTE
    )
}

// TODO: Add boosts
fun advanceOneMinute(state: FarmState, constants: Constants, elapsed: Duration = Duration.ZERO): FarmState = state.copy(
    eggsLaid = state.eggsLaid + eggIncrease(state.habs, constants),
    habs = state.habs.map { hab ->
        val internalHatcherySharingMultiplier = state.habs.fold(BigDecimal.ONE) { acc, (population, capacity) ->
            if (population == capacity) acc + BigDecimal.ONE else acc
        }.multiply(constants.internalHatcherySharing)
        hab.copy(
            population = minOf(
                hab.population + constants.internalHatcheryRate.multiply(internalHatcherySharingMultiplier),
                hab.capacity
            )
        )
    },
    habBottleneck = state.habBottleneck ?: habBottleneck(state.habs, elapsed),
    transportBottleneck = state.transportBottleneck ?: transportBottleneck(state.habs, constants, elapsed)
)

fun transportBottleneck(habs: List<Hab>, constants: Constants, elapsed: Duration): Duration? {
    return when {
        eggIncrease(habs, constants) >= constants.transportRate -> elapsed
        else -> null
    }
}

fun habBottleneck(habs: List<Hab>, elapsed: Duration): Duration? {
    return when {
        habs.all { (population, capacity) -> population == capacity } -> elapsed
        else -> null
    }
}

fun eggIncrease(habs: List<Hab>, constants: Constants): BigDecimal = minOf(
    habs.sumByBigDecimal(Hab::population).multiply(EGG_LAYING_BASE_RATE).multiply(constants.eggLayingBonus),
    constants.transportRate
)

fun willReachBottlenecks(state: FarmState, finalGoalReachedAt: Duration?): Boolean {
    val bottlenecks = listOf(state.habBottleneck, state.transportBottleneck)
    val reachesBottlenecks = bottlenecks.filterNotNull().any()
    return when {
        state.habBottleneck == null && state.transportBottleneck == null -> false
        reachesBottlenecks && finalGoalReachedAt == null -> true
        reachesBottlenecks && bottlenecks.any { it?.isShorterThan(finalGoalReachedAt) == true } -> true
        else -> false
    }
}