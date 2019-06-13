package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.MathContext.DECIMAL64
import java.util.*

class CoopContractSimulation private constructor(
    backups: List<EggInc.Backup>,
    val coopStatus: EggInc.CoopStatusResponse
) {
    val log = KotlinLogging.logger { }

    val localContract: EggInc.LocalContract = backups.filter { backup ->
        backup.contracts.contractsList.plus(backup.contracts.archiveList).any { contract ->
            contract.contract.identifier == coopStatus.contractIdentifier
        }
    }.maxBy { backup ->
        backup.approxTime
    }!!.let { backup ->
        backup.contracts.contractsList.plus(backup.contracts.archiveList).find { contract ->
            contract.contract.identifier == coopStatus.contractIdentifier
        }!!
    }

    val farms: List<ContractSimulation> = backups.filter { backup ->
        backup.farmsList.any { farm ->
            farm.contractId == coopStatus.contractIdentifier
        }
    }.map { backup ->
        ContractSimulation(backup, localContract).also {
            it.isActive = coopStatus.contributorsList.find { contributor ->
                contributor.userId == backup.userid
            }?.active == 1
        }
    }

    //
    // Basic info
    //

    val contractId: String by lazy { localContract.contract.identifier }
    val contractName: String by lazy { localContract.contract.name }
    val coopId: String by lazy { localContract.coopIdentifier }
    val egg: EggInc.Egg by lazy { localContract.contract.egg }
    val maxCoopSize by lazy { localContract.contract.maxCoopSize }

    //
    // Totals
    //

    val eggsLaid: BigDecimal by lazy { farms.sumBy { farm -> farm.eggsLaid } }
    val eggLayingRatePerSecond: BigDecimal by lazy { farms.sumBy { farm -> farm.eggLayingRatePerSecond } }
    val population: BigDecimal by lazy { farms.sumBy { farm -> farm.population } }
    val populationIncreaseRatePerSecond: BigDecimal by lazy { farms.sumBy { farm -> farm.populationIncreaseRatePerSecond } }
    val populationIncreaseRatePerMinute: BigDecimal by lazy { farms.sumBy { farm -> farm.populationIncreaseRatePerMinute } }
    val populationIncreaseRatePerHour: BigDecimal by lazy { farms.sumBy { farm -> farm.populationIncreaseRatePerHour } }
    val eggLayingRatePerMinute: BigDecimal by lazy { farms.sumBy { farm -> farm.eggLayingRatePerMinute } }
    val eggLayingRatePerHour: BigDecimal by lazy { farms.sumBy { farm -> farm.eggLayingRatePerMinute } }
    val currentEggLayingRatePerSecond: BigDecimal by lazy { farms.sumBy { farm -> farm.currentEggLayingRatePerSecond } }
    val currentEggLayingRatePerMinute: BigDecimal by lazy { farms.sumBy { farm -> farm.currentEggLayingRatePerMinute } }

    //
    // Contract details
    //

    val elapsedTime: Duration by lazy { Duration(localContract.timeAccepted.toDateTime(), DateTime.now()) }

    val timeRemaining: Duration by lazy { coopStatus.secondsRemaining.toDuration() }

    val goals: SortedMap<Int, BigDecimal> by lazy {
        localContract.contract.goalsList
            .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
            .toMap()
            .toSortedMap()
    }

    val finalGoal: BigDecimal by lazy { goals[goals.lastKey()]!! }


    //
    //  Projection
    //

    fun timeTo(goal: BigDecimal): Duration = (goal - eggsLaid).coerceAtLeast(ZERO)
        .divide(eggLayingRatePerSecond, DECIMAL64).toLong().toDuration()

    fun timeToFinalGoal(): Duration = timeTo(finalGoal)

    val accelerationFactor: BigDecimal? by lazy {
        if (population == ZERO) null
        else (eggLayingRatePerSecond * (population + populationIncreaseRatePerSecond)
            .divide(population, DECIMAL64) - eggLayingRatePerSecond)
            .divide(ONE, DECIMAL64)
    }

    // TODO: Take bottlenecks into account
    // TODO: Take Internal Hatchery Calm into account
    fun projectedTimeTo(goal: BigDecimal): Duration? = accelerationFactor?.let { accelerationFactor ->
        (eggLayingRatePerSecond.negate() + sqrt(
            eggLayingRatePerSecond.pow(2) + BigDecimal(2) * accelerationFactor * (goal - eggsLaid).coerceAtLeast(ONE)
        )).divide(accelerationFactor, DECIMAL64).toLong().toDuration()
    }

    fun projectedTimeToFinalGoal(): Duration? = projectedTimeTo(finalGoal)

    fun projectedToFinish(): Boolean = projectedTimeToFinalGoal()?.let { it < timeRemaining } == true

    companion object Factory {
        operator fun invoke(contractId: String, coopId: String): CoopContractSimulationResult {
            val (coopStatus, error) = AuxBrain.getCoopStatus(contractId, coopId)
            val contractName: String? = AuxBrain.getContracts().contractsList.find { contract ->
                contract.identifier == contractId
            }!!.name

            // Co-op not found?
            if (coopStatus == null || error != null) return NotFound(contractId, coopId)

            val backups: List<EggInc.Backup> = runBlocking(Dispatchers.IO) {
                coopStatus.contributorsList.asyncMap { AuxBrain.getFarmerBackup(it.userId) }
            }.mapNotNull {
                it.component1()
            }

            // Co-op finished?
            if (backups.any { contributor ->
                    contributor.farmsList.none { farm ->
                        farm.contractId == coopStatus.contractIdentifier
                    } && contributor.contracts.archiveList.find { contract ->
                        contract.contract.identifier == coopStatus.contractIdentifier
                    }?.let { contract ->
                        contract.finished
                    } == true
                }
            ) return Finished(coopStatus, contractName!!)

            // Co-op not empty?
            if (backups.none { backup ->
                    backup.farmsList.any { farm ->
                        farm.contractId == coopStatus.contractIdentifier
                    }
                }
            ) return Empty(coopStatus, contractName!!)

            // Co-op in progress
            return InProgress(CoopContractSimulation(backups, coopStatus))
        }
    }
}
