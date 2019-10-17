package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.util.*

class CoopContractSimulation private constructor(
    backups: List<EggInc.Backup>,
    val coopStatus: EggInc.CoopStatusResponse
) {
    val log = KotlinLogging.logger { }

    val localContract: EggInc.LocalContract = backups.findContract(coopStatus.contractId)!!

    val farms: List<ContractSimulation> = backups.filter { backup ->
        backup.farmsList.any { farm ->
            farm.contractId == coopStatus.contractId
        }
    }.map { backup ->
        ContractSimulation(backup, localContract).also {
            it.isActive = coopStatus.contributorsList.find { contributor ->
                contributor.userId == backup.userId
            }?.active == true
        }
    }

    //
    // Basic info
    //

    val contractId: String by lazy { localContract.contract.id }
    val contractName: String by lazy { localContract.contract.name }
    val coopId: String by lazy { localContract.coopId }
    val egg: EggInc.Egg by lazy { localContract.contract.egg }
    val maxCoopSize by lazy { localContract.contract.maxCoopSize }

    //
    // Totals
    //

    val eggsLaid: BigDecimal by lazy { farms.sumBy { farm -> farm.eggsLaid } }
    val population: BigDecimal by lazy { farms.sumBy { farm -> farm.population } }
    val populationIncreaseRatePerMinute: BigDecimal by lazy { farms.sumBy { farm -> farm.populationIncreasePerMinute } }
    val populationIncreaseRatePerHour: BigDecimal by lazy { farms.sumBy { farm -> farm.populationIncreasePerHour } }
    val eggLayingRatePerHour: BigDecimal by lazy { farms.sumBy { farm -> farm.eggsLaidPerHour } }

    //
    // Contract details
    //

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

    // TODO: Calculate time to multiple goals in one go
    fun projectedTimeTo(goal: BigDecimal): Duration? = if (population == ZERO) null else {
        var projectedEggsLaid = eggsLaid
        var duration = Duration.ZERO

        do {
            // TODO: Move 'step' method to ContractSimulation
            // farms.forEach { farm ->
            //     projectedEggsLaid += farm.projectedEggLayingRatePerMinute
            //     if (farm.projectedPopulation < farm.habsMaxCapacity) farm.projectedPopulation =
            //         (farm.projectedPopulation + populationIncreaseRatePerMinute).coerceAtMost(farm.habsMaxCapacity)
            // }
            duration += Duration.standardSeconds(60)
        } while (projectedEggsLaid < goal)

        duration
    }

    fun projectedTimeToFinalGoal(): Duration? = projectedTimeTo(finalGoal)

    fun projectedToFinish(): Boolean = projectedTimeToFinalGoal()?.let { it < timeRemaining } == true

    //
    // Object Factory
    //

    companion object Factory {
        operator fun invoke(contractId: String, coopId: String): CoopContractSimulationResult {
            val (coopStatus, error) = AuxBrain.getCoopStatus(contractId, coopId)
            val contractName: String? = AuxBrain.getPeriodicals()?.contracts?.contractsList?.find { contract ->
                contract.id == contractId
            }?.name

            // Co-op not found?
            if (coopStatus == null || error != null) return NotFound(contractId, coopId)

            val backups: List<EggInc.Backup> = runBlocking(Dispatchers.IO) {
                coopStatus.contributorsList.asyncMap { AuxBrain.getFarmerBackup(it.userId) }
            }.mapNotNull {
                it.component1()
            }

            // Co-op not empty?
            if (backups.none { backup ->
                    backup.farmsList.any { farm ->
                        farm.contractId == coopStatus.contractId
                    }
                }
            ) return Empty(coopStatus, contractName!!)

            val contract = backups.findContract(contractId)

            //
            // Co-op finished?
            //
            // The amount of eggs laid according to the co-op status is higher than the final goal
            // or
            // There is no active farm with this contract
            // and the contract archive contains this contract
            // and that contract has reached its final goal
            // for any of the contributors
            if (coopStatus.eggsLaid >= contract?.finalGoal ?: ZERO || backups.any { contributor ->
                    contributor.farmsList.none { farm ->
                        farm.contractId == coopStatus.contractId
                    } && contributor.contracts.archiveList.find { contract ->
                        contract.contract.id == coopStatus.contractId
                    }?.finished == true
                }
            ) return Finished(coopStatus, contractName!!)

            // Co-op in progress
            return InProgress(CoopContractSimulation(backups, coopStatus))
        }
    }
}
