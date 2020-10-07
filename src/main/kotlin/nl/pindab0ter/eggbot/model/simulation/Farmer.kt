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
        reportedState: FarmState,
        caughtUpState: FarmState? = null,
        constants: Constants,
        timeSinceBackup: Duration,
    ) : this(
        name = name,
        reportedState = reportedState,
        caughtUpState = caughtUpState,
        runningState = caughtUpState ?: reportedState,
        goalsReachedState = null,
        timeUpState = null,
        constants = constants,
        timeSinceBackup = timeSinceBackup
    )

    val reportedEggsLaid: BigDecimal get() = reportedState.eggsLaid
    val caughtUpEggsLaid: BigDecimal get() = caughtUpState?.eggsLaid ?: BigDecimal.ZERO
    val currentState = caughtUpState ?: reportedState
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
        ): Farmer? {
            val farm = backup.farmFor(contractId) ?: return null
            val constants = Constants(backup, farm)
            val reportedState = FarmState(farm, constants)
            return Farmer(
                name = backup.userName,
                reportedState = reportedState,
                caughtUpState = catchUp(reportedState, constants, minOf(backup.timeSinceBackup, constants.maxAwayTime)),
                constants = constants,
                timeSinceBackup = backup.timeSinceBackup
            )
        }
    }
}