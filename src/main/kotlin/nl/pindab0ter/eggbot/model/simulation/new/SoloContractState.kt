package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Egg
import nl.pindab0ter.eggbot.helpers.asDaysHoursAndMinutes
import org.joda.time.Duration
import java.math.BigDecimal

data class SoloContractState(
    val contractId: String,
    val contractName: String,
    val egg: Egg,
    val goals: Set<Goal>,
    val timeRemaining: Duration,
    val elapsed: Duration = Duration.ZERO,
    val eggspected: BigDecimal = BigDecimal.ZERO,
    val farmer: Farmer,
) {
    val finished: Boolean get() = goals.all { (_, moment) -> moment == Duration.ZERO }
    val goalsReached: Int get() = goals.count { (_, moment) -> moment != null }

    override fun toString(): String = "${this::class.simpleName}(" +
            "contractId=${contractId}, " +
            "contractName=${contractName}, " +
            "goals=${goals}, " +
            "timeRemaining=${timeRemaining.asDaysHoursAndMinutes()}, " +
            "elapsed=${elapsed.asDaysHoursAndMinutes()}" +
            ")"
}