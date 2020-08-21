package nl.pindab0ter.eggbot.model.simulation.old

import org.joda.time.Duration
import java.math.BigDecimal

data class GoalReachedMoment(
    val target: BigDecimal,
    var moment: Duration?
) : Comparable<GoalReachedMoment> {
    override fun compareTo(other: GoalReachedMoment): Int = this.target.compareTo(other.target)
}