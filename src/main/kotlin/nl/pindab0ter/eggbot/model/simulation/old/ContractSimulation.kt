package nl.pindab0ter.eggbot.model.simulation.old

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Egg
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.ONE_YEAR
import nl.pindab0ter.eggbot.helpers.finished
import nl.pindab0ter.eggbot.helpers.toDateTime
import nl.pindab0ter.eggbot.helpers.toDuration
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.util.*

class ContractSimulation constructor(
    backup: Backup,
    override val farm: Backup.Simulation,
    localContract: LocalContract
) : Simulation(backup) {
    // region Basic info
    val contractId: String = localContract.contract!!.id
    val contractName: String = localContract.contract!!.name
    val egg: Egg = localContract.contract!!.egg
    var isActive: Boolean = true
    val isFinished: Boolean = localContract.finished
    val timeRemaining: Duration = localContract.contract!!.lengthSeconds.toDuration()
        .minus(Duration(localContract.timeAccepted.toDateTime(), DateTime.now()))
    val goals: SortedSet<BigDecimal> = localContract.contract!!.goals.map { goal ->
        goal.targetAmount.toBigDecimal()
    }.toSortedSet()

    // endregion Basic info

    // region Simulation

    override var elapsed: Duration = Duration.ZERO
    override val currentEggs: BigDecimal = farm.eggsLaid.toBigDecimal()
    override var projectedEggs: BigDecimal = currentEggs
    override val currentPopulation: BigDecimal = farm.habPopulation.sum().toBigDecimal()
    override var projectedPopulation: BigDecimal = currentPopulation
    override lateinit var eggspected: BigDecimal
    var habBottleneckReached: Duration? = null
        get() = if (field != null && field!! < timeRemaining) field else null
    var transportBottleneckReached: Duration? = null
        get() = if (field != null && field!! < timeRemaining) field else null
    val goalReachedMoments: SortedSet<GoalReachedMoment> = goals.map { goal ->
        GoalReachedMoment(goal, if (currentEggs >= goal) Duration.ZERO else null)
    }.toSortedSet()
    private val currentGoal: GoalReachedMoment?
        get() = goalReachedMoments.filter { it.moment == null }.minByOrNull { it.target }
    private val projectedEggsPerMinute: BigDecimal
        get() = projectedPopulation * eggsPerChickenPerMinute
    val goalsReached: Int
        get() = goalReachedMoments.count { (_, moment) -> moment?.let { it < timeRemaining } == true }

    fun step() {
        elapsed += Duration.standardMinutes(1)
        if (habBottleneckReached == null && projectedPopulation >= habsMaxCapacity)
            habBottleneckReached = elapsed
        if (transportBottleneckReached == null && projectedEggsPerMinute >= shippingRatePerMinute)
            transportBottleneckReached = elapsed
        if (currentGoal != null && projectedEggs >= currentGoal!!.target)
            currentGoal!!.moment = elapsed
        if (!this::eggspected.isInitialized && elapsed >= timeRemaining)
            eggspected = projectedEggs
        projectedEggs += projectedEggsPerMinute.coerceAtMost(shippingRatePerMinute)
        projectedPopulation = projectedPopulation.plus(populationIncreasePerMinute).coerceAtMost(habsMaxCapacity)
    }

    fun run() {
        do step() while (
            elapsed <= timeRemaining ||                                            // Time limit hasn't been reached or
            (goalReachedMoments.any { it.moment == null } && elapsed <= ONE_YEAR) // the goals haven't been reached and a year hasn't yet passed
        )
    }

    // endregion Simulation

    companion object {
        operator fun invoke(
            backup: Backup,
            contractId: String
        ): ContractSimulation? {
            val contract = backup.contracts?.contracts?.find { localContract ->
                localContract.contract?.id == contractId
            }
            val farm = backup.farms.find { it.contractId == contract?.contract?.id }
            return if (contract != null && farm != null) ContractSimulation(backup, farm, contract)
            else null
        }
    }
}