package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FOUR
import org.joda.time.Duration
import java.math.BigDecimal

data class Farmer(
    val name: String,
    val reportedState: FarmState,
    val currentState: FarmState?,
    val runningState: FarmState,
    val goalsReachedState: FarmState?,
    val timeUpState: FarmState?,
    val constants: Constants,
    val timeSinceBackup: Duration,
) {
    constructor(
        name: String,
        reportedState: FarmState,
        currentState: FarmState? = null,
        constants: Constants,
        timeSinceBackup: Duration,
    ) : this(
        name = name,
        reportedState = reportedState,
        currentState = currentState,
        runningState = currentState ?: reportedState,
        goalsReachedState = null,
        timeUpState = null,
        constants = constants,
        timeSinceBackup = timeSinceBackup
    )

    val reportedEggsLaid: BigDecimal get() = reportedState.eggsLaid
    val currentEggsLaid: BigDecimal get() = currentState?.eggsLaid ?: BigDecimal.ZERO
    val currentEggsPerMinute: BigDecimal
        get() = when {
            awayTimeRemaining <= Duration.ZERO -> BigDecimal.ZERO
            else -> eggIncrease(currentState?.habs ?: emptyList(), constants)
        }
    val currentChickens: BigDecimal get() = currentState?.population ?: BigDecimal.ZERO
    val currentChickenIncreasePerMinute: BigDecimal
        get() = chickenIncrease(currentState?.habs ?: emptyList(),
            constants).multiply(FOUR - (currentState?.habs?.fullCount() ?: BigDecimal.ZERO))
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
                currentState = catchUp(reportedState, constants, minOf(backup.timeSinceBackup, constants.maxAwayTime)),
                constants = constants,
                timeSinceBackup = backup.timeSinceBackup
            )
        }
    }
}