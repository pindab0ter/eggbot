package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.helpers.asDaysHoursAndMinutes
import nl.pindab0ter.eggbot.helpers.asIllions
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
        fun fromContract(contract: Contract, eggsLaid: BigDecimal): Set<Goal> =
            contract.goals.map { goal ->
                Goal(
                    target = goal.targetAmount.toBigDecimal(),
                    moment = if (eggsLaid >= goal.targetAmount.toBigDecimal()) Duration.ZERO else null
                )
            }.toSet()
    }
}