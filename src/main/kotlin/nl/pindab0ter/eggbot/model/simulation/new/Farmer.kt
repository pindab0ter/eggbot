package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.catchUp
import nl.pindab0ter.eggbot.helpers.farmFor
import nl.pindab0ter.eggbot.helpers.timeSinceBackup
import org.joda.time.Duration

data class Farmer(
    val name: String,
    val initialState: FarmState,
    val finalState: FarmState,
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
        initialState = state,
        finalState = state,
        constants = constants,
        timeSinceBackup = timeSinceBackup
    )

    val awayTimeRemaining: Duration get() = constants.maxAwayTime - timeSinceBackup
    val isSleeping: Boolean get() = awayTimeRemaining <= Duration.ZERO

    companion object {
        operator fun invoke(backup: Backup, contractId: String, catchUp: Boolean): Farmer? {
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