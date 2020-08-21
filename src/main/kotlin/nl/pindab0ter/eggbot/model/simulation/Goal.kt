package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.utilities.asDaysHoursAndMinutes
import nl.pindab0ter.eggbot.utilities.asIllions
import org.joda.time.Duration
import java.math.BigDecimal

data class Goal(
    val target: BigDecimal,
    val moment: Duration? = null,
) {
    override fun toString(): String = "${this::class.simpleName}(" +
            "target=${target.asIllions()}, " +
            "moment=${moment?.asDaysHoursAndMinutes()}" +
            ")"

    companion object {
        fun fromContract(localContract: LocalContract, eggsLaid: Double) = localContract.contract!!.goals.map { goal ->
            Goal(
                target = goal.targetAmount.toBigDecimal(),
                moment = if (eggsLaid >= goal.targetAmount) Duration.ZERO else null
            )
        }
    }
}