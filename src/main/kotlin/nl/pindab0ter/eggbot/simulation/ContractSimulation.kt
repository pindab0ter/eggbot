package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import mu.KotlinLogging
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.util.*

class ContractSimulation constructor(
    backup: EggInc.Backup,
    private val localContract: EggInc.LocalContract
) : Simulation(backup) {

    val log = KotlinLogging.logger { }

    override val farm: EggInc.Backup.Simulation =
        backup.farmsList.find { it.contractId == localContract.contract.id }!!

    //
    // Basic info
    //

    val contractId: String = localContract.contract.id
    val contractName: String = localContract.contract.name
    val egg: EggInc.Egg = localContract.contract.egg
    var isActive: Boolean = true
    val finished: Boolean = localContract.finished
    override val population: BigDecimal = farm.habPopulation.sum()
    var projectedPopulation: BigDecimal = population
    override val eggsLaid: BigDecimal = farm.eggsLaid.toBigDecimal()
    var projectedEggsLaid: BigDecimal = eggsLaid
    val projectedEggLayingRatePerMinute: BigDecimal = eggLayingRatePerChickenPerSecond * projectedPopulation * 60

    //
    // Contract details
    //

    val elapsedTime: Duration = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())

    val timeRemaining: Duration = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)

    val goals: SortedMap<Int, BigDecimal> = localContract.contract.goalsList
        .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
        .toMap()
        .toSortedMap()

    val finalGoal: BigDecimal = goals[goals.lastKey()]!!

    //
    //  Projection
    //

    // TODO: Calculate multiple goals in one go
    // TODO: Determine which bottlenecks have been reached
    fun projectedTimeTo(goal: BigDecimal): Duration? = if (population == ZERO) null else {
        var duration = Duration.ZERO

        do {
            projectedEggsLaid += projectedEggLayingRatePerMinute
            if (population < habsMaxCapacity) projectedPopulation =
                (population + populationIncreaseRatePerMinute).coerceAtMost(habsMaxCapacity)
            duration += Duration.standardSeconds(60)
        } while (projectedEggsLaid < goal)

        duration
    }

    fun projectedTimeToFinalGoal(): Duration? = projectedTimeTo(finalGoal)

    fun projectedToFinish(): Boolean = projectedTimeToFinalGoal()?.let { it < timeRemaining } == true

    companion object {
        operator fun invoke(
            backup: EggInc.Backup,
            contractId: String
        ): ContractSimulation? = backup.contracts.contractsList.find { localContract ->
            localContract.contract.id == contractId
        }?.let { contract ->
            ContractSimulation(backup, contract)
        }
    }
}
