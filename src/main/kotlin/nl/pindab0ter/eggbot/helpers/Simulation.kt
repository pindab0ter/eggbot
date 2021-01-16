package nl.pindab0ter.eggbot.helpers

import nl.pindab0ter.eggbot.helpers.HabsStatus.*
import nl.pindab0ter.eggbot.model.simulation.Constants
import nl.pindab0ter.eggbot.model.simulation.FarmState
import nl.pindab0ter.eggbot.model.simulation.Farmer
import nl.pindab0ter.eggbot.model.simulation.Hab
import org.joda.time.Duration
import java.math.BigDecimal

/** Base egg laying rate per chicken per minute.
 *
 * A chicken lays 1/30 of an egg per second, so 2 per minute */
private val EGG_LAYING_BASE_RATE = BigDecimal(2)
private val MAX_HABS_CAPACITY = BigDecimal(2835000000L)

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

fun advanceOneMinute(state: FarmState, constants: Constants, elapsed: Duration = Duration.ZERO): FarmState = state.copy(
    eggsLaid = state.eggsLaid + eggIncrease(state.habs, constants),
    habs = state.habs.map { hab ->
        hab.copy(population = minOf(hab.population + chickenIncrease(state.habs, constants), hab.capacity))
    },
    habsStatus = when (state.habsStatus) {
        Free -> habsStatus(state.habs, elapsed)
        else -> state.habsStatus
    },
    transportBottleneck = state.transportBottleneck ?: transportBottleneck(state.habs, constants, elapsed)
)

fun transportBottleneck(habs: List<Hab>, constants: Constants, elapsed: Duration): Duration? {
    return when {
        eggIncrease(habs, constants) >= constants.transportRate -> elapsed
        else -> null
    }
}

fun habsStatus(habs: List<Hab>, elapsed: Duration): HabsStatus {
    return when {
        habs.all { (population, capacity) -> population equals MAX_HABS_CAPACITY && capacity equals MAX_HABS_CAPACITY } ->
            MaxedOut(elapsed)
        habs.all { (population, capacity) -> population == capacity } ->
            BottleneckReached(elapsed)
        else -> Free
    }
}

fun chickenIncrease(habs: List<Hab>, constants: Constants): BigDecimal =
    constants.hatcheryRate.multiply(
        BigDecimal.ONE + habs.fullCount().multiply(constants.hatcherySharing)
    )

fun List<Hab>.fullCount(): BigDecimal = sumOf { (population, capacity) ->
    if (population == capacity) BigDecimal.ONE
    else BigDecimal.ZERO
}

fun eggIncrease(habs: List<Hab>, constants: Constants): BigDecimal = minOf(
    habs.sumOf { it.population }.multiply(EGG_LAYING_BASE_RATE).multiply(constants.eggLayingBonus),
    constants.transportRate
)

fun willReachBottleneckBeforeDone(farmer: Farmer, timeRemaining: Duration, finalGoalReachedAt: Duration?): Boolean {
    val firstBottleneckReachedAt: List<Duration> = listOfNotNull(
        if (farmer.runningState.habsStatus is BottleneckReached) farmer.runningState.habsStatus.moment else null,
        farmer.runningState.transportBottleneck,
        farmer.awayTimeRemaining.let { moment -> if (moment < Duration.standardHours(12)) moment else null }
    )

    return when {
        farmer.runningState.habsStatus is MaxedOut && farmer.runningState.habsStatus.moment == Duration.ZERO -> true
        firstBottleneckReachedAt.isEmpty() -> false
        else -> firstBottleneckReachedAt.minOrNull()!! < listOfNotNull(timeRemaining, finalGoalReachedAt).minOrNull()!!
    }
}

interface HabsStatus {
    object Free : HabsStatus
    class BottleneckReached(val moment: Duration) : HabsStatus
    class MaxedOut(val moment: Duration) : HabsStatus
}
