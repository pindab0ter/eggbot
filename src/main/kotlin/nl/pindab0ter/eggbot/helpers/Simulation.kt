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

// TODO: Add silos check
// TODO: Add max away time
tailrec fun catchUp(
    state: FarmState,
    timeSinceLastBackup: Duration
): FarmState = when {
    timeSinceLastBackup <= Duration.ZERO -> state
    else -> catchUp(
        state = advanceOneMinute(state),
        timeSinceLastBackup = timeSinceLastBackup - ONE_MINUTE
    )
}

// TODO: Add boosts
fun advanceOneMinute(state: FarmState, elapsed: Duration = Duration.ZERO): FarmState = state.copy(
    eggsLaid = state.eggsLaid + minOf(eggIncrease(state.habs, state.constants), state.constants.transportRate),
    habs = state.habs.map { hab ->
        val internalHatcherySharingMultiplier = state.habs.fold(BigDecimal.ONE) { acc, (population, capacity) ->
            if (population == capacity) acc + BigDecimal.ONE else acc
        }.multiply(state.constants.internalHatcherySharing)
        hab.copy(
            population = minOf(
                hab.population + state.constants.internalHatcheryRate.multiply(internalHatcherySharingMultiplier),
                hab.capacity
            )
        )
    },
    habBottleneck = state.habBottleneck ?: habBottleneck(state.habs, elapsed),
    transportBottleneck = state.transportBottleneck ?: transportBottleneck(state.habs, state.constants, elapsed)
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

fun eggIncrease(habs: List<Hab>, constants: Constants): BigDecimal =
    habs.sumByBigDecimal(Hab::population).multiply(EGG_LAYING_BASE_RATE).multiply(constants.eggLayingBonus)
