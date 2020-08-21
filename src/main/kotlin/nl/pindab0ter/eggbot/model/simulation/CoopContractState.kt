package nl.pindab0ter.eggbot.model.simulation

import nl.pindab0ter.eggbot.utilities.asDaysHoursAndMinutes
import nl.pindab0ter.eggbot.utilities.sumByBigDecimal
import org.joda.time.Duration
import java.math.BigDecimal

data class CoopContractState(
    val contractId: String,
    val contractName: String,
    val coopId: String,
    val goals: List<Goal>,
    val timeRemaining: Duration,
    val elapsed: Duration = Duration.ZERO,
    val farmers: List<Farmer>,
) {
    val eggsLaid: BigDecimal get() = farmers.sumByBigDecimal { farmer -> farmer.finalState.eggsLaid }

    override fun toString(): String = "${this::class.simpleName}(" +
            "contractId=${contractId}, " +
            "contractName=${contractName}, " +
            "coopId=${coopId}, " +
            "goals=${goals}, " +
            "timeRemaining=${timeRemaining.asDaysHoursAndMinutes()}, " +
            "elapsed=${elapsed.asDaysHoursAndMinutes()}" +
            ")"
}