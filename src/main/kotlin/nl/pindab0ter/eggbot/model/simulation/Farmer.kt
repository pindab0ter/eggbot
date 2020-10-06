package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FOUR
import org.joda.time.Duration
import java.math.BigDecimal

data class Farmer(
    val name: String,
    val reportedState: FarmState,
    val caughtUpState: FarmState?,
    val runningState: FarmState,
    val goalsReachedState: FarmState?,
    val timeUpState: FarmState?,
    val constants: Constants,
    val timeSinceBackup: Duration,
) {
    constructor(
        name: String,
        state: FarmState,
        constants: Constants,
        timeSinceBackup: Duration,
    ) : this(
        name = name,
        reportedState = state,
        caughtUpState = state,
        runningState = state,
        goalsReachedState = null,
        timeUpState = null,
        constants = constants,
        timeSinceBackup = timeSinceBackup
    )

    val currentState = caughtUpState ?: reportedState
    val currentEggsLaid: BigDecimal get() = currentState.eggsLaid
    val currentEggsPerMinute: BigDecimal
        get() = when {
            awayTimeRemaining <= Duration.ZERO -> BigDecimal.ZERO
            else -> eggIncrease(currentState.habs, constants)
        }
    val currentChickens: BigDecimal get() = currentState.population
    val currentChickenIncreasePerMinute: BigDecimal
        get() = chickenIncrease(currentState.habs,
            constants).multiply(FOUR - currentState.habs.fullCount())
    val timeUpEggsLaid: BigDecimal get() = timeUpState?.eggsLaid ?: BigDecimal.ZERO
    val awayTimeRemaining: Duration get() = constants.maxAwayTime - timeSinceBackup
    val isSleeping: Boolean get() = awayTimeRemaining <= Duration.ZERO

    companion object {
        operator fun invoke(
            backup: Backup,
            contractId: String,
            catchUp: Boolean,
        ): Farmer? {
            val farm = backup.farmFor(contractId) ?: return null
            val constants = Constants(backup, farm)
            val reportedState = FarmState(farm, constants)
            return if (catchUp) Farmer(
                name = backup.userName,
                state = catchUp(reportedState, constants, minOf(backup.timeSinceBackup, constants.maxAwayTime)),
                constants = constants,
                timeSinceBackup = backup.timeSinceBackup
            ) else Farmer(
                name = backup.userName,
                state = reportedState,
                constants = constants,
                timeSinceBackup = backup.timeSinceBackup
            )
        }
    }
}