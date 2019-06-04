package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.sqrt
import nl.pindab0ter.eggbot.sumBy
import nl.pindab0ter.eggbot.toDateTime
import nl.pindab0ter.eggbot.toDuration
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.MathContext.DECIMAL64
import java.util.*

class CoopContractSimulation constructor(
    backups: List<EggInc.Backup>,
    private val localContract: EggInc.LocalContract
) {

    val farms: List<ContractSimulation> = backups.map { backup ->
        ContractSimulation(backup, localContract)
    }

    //
    // Basic info
    //

    val contractId: String = localContract.contract.identifier
    val contractName: String = localContract.contract.name
    val egg: EggInc.Egg = localContract.contract.egg


    //
    // Totals
    //

    val eggsLaid: BigDecimal = farms.sumBy { farm -> farm.eggsLaid }
    val eggLayingRatePerSecond: BigDecimal = farms.sumBy { farm -> farm.eggLayingRatePerSecond }
    val population: BigDecimal = farms.sumBy { farm -> farm.population }
    val populationIncreaseRatePerSecond: BigDecimal = farms.sumBy { farm -> farm.populationIncreaseRatePerSecond }
    val populationIncreaseRatePerMinute: BigDecimal = farms.sumBy { farm -> farm.populationIncreaseRatePerMinute }
    val eggLayingRatePerMinute: BigDecimal = farms.sumBy { farm -> farm.eggLayingRatePerMinute }
    val currentEggLayingRatePerMinute: BigDecimal = farms.sumBy { farm -> farm.currentEggLayingRatePerMinute }

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

    val timeRequired: Duration by lazy {
        (finalGoal - eggsLaid)
            .divide(eggLayingRatePerSecond, DECIMAL64).toLong().toDuration()
    }

    val accelerationFactor: BigDecimal by lazy {
        (eggLayingRatePerSecond * (population + populationIncreaseRatePerSecond)
            .divide(population, DECIMAL64) - eggLayingRatePerSecond).divide(ONE, DECIMAL64)
    }

    // TODO: Take bottlenecks into account
    val projectedTimeRequired: Duration by lazy {
        (eggLayingRatePerSecond * BigDecimal(-1L) + sqrt(eggLayingRatePerSecond.pow(2) + BigDecimal(2) * accelerationFactor * (finalGoal - eggsLaid)))
            .divide(accelerationFactor, DECIMAL64).toLong().toDuration()
    }

    companion object {
        operator fun invoke(
            backups: List<EggInc.Backup>,
            contractId: String
        ): CoopContractSimulation? = backups.first().contracts.contractsList.find { localContract ->
            localContract.contract.identifier == contractId
        }?.let { contract ->
            CoopContractSimulation(backups, contract)
        }
    }
}
