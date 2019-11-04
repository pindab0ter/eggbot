package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.Duration
import org.joda.time.Duration.ZERO
import java.math.BigDecimal
import java.util.*

class CoopContractSimulation private constructor(
    backups: List<EggInc.Backup>,
    val coopStatus: EggInc.CoopStatusResponse
) {

    val log = KotlinLogging.logger { }

    // region Basic info

    val localContract: EggInc.LocalContract = backups.findContract(coopStatus.contractId)!!

    val farms: List<ContractSimulation> = backups.filter { backup ->
        backup.farmsList.any { farm -> farm.contractId == coopStatus.contractId }
    }.mapNotNull { backup ->
        ContractSimulation(backup, localContract.contract.id).also {
            // TODO: Save this information somewhere else
            it?.isActive = coopStatus.contributorsList.find { contributor ->
                contributor.userId == backup.userId
            }?.active == true
        }
    }
    val contractId: String get() = localContract.contract.id
    val contractName: String get() = localContract.contract.name
    val coopId: String get() = localContract.coopId
    val egg: EggInc.Egg get() = localContract.contract.egg
    val maxCoopSize: Int get() = localContract.contract.maxCoopSize
    val timeRemaining: Duration get() = coopStatus.secondsRemaining.toDuration()
    val goals: SortedSet<BigDecimal> = localContract.contract.goalsList.map { goal ->
        goal.targetAmount.toBigDecimal()
    }.toSortedSet()
    val populationIncreaseRatePerHour: BigDecimal get() = farms.sumBy { farm -> farm.populationIncreasePerHour }

    // endregion Basic info

    // region Simulation
    val currentEggsPerHour: BigDecimal by lazy { farms.sumBy { it.currentEggsPerHour } }

    var elapsed: Duration = ZERO
    val currentEggs: BigDecimal = farms.sumBy { it.currentEggs }
    private val projectedEggs: BigDecimal get() = farms.sumBy { it.projectedEggs }
    val currentPopulation: BigDecimal = farms.sumBy { it.currentPopulation }
    lateinit var eggspected: BigDecimal
    val goalReachedMoments: SortedSet<GoalReachedMoment> = goals.map { goal ->
        GoalReachedMoment(goal, if (currentEggs >= goal) ZERO else null)
    }.toSortedSet()
    private val currentGoal: GoalReachedMoment? get() = goalReachedMoments.filter { it.moment == null }.minBy { it.target }
    val willFinish: Boolean get() = goalReachedMoments.maxBy { it.target }?.moment?.let { it < timeRemaining } == true
    val populationIncreasePerHour: BigDecimal get() = farms.sumBy { it.populationIncreasePerHour }
    val eggsPerHour: BigDecimal get() = farms.sumBy { it.eggsPerChickenPerMinute * currentPopulation * 60 }

    private fun step() {
        if (currentGoal != null && projectedEggs >= currentGoal!!.target)
            currentGoal!!.moment = elapsed
        if (!this::eggspected.isInitialized && elapsed >= timeRemaining)
            eggspected = projectedEggs
        farms.forEach { it.step() }
        elapsed += Duration.standardMinutes(1)
    }

    fun run() {
        do step() while (
            (goalReachedMoments.any { it.moment == null } && elapsed < timeRemaining) // Not all goals have been reached in time
            || elapsed < ONE_YEAR                                                     // Or one year hasn't yet passed
        )
    }

    // endregion

    companion object Factory {
        operator fun invoke(contractId: String, coopId: String): CoopContractSimulationResult {
            val (coopStatus, error) = AuxBrain.getCoopStatus(contractId, coopId)
            val contractName: String? =
                AuxBrain.getPeriodicals()?.contracts?.contractsList?.find { contract ->
                    contract.id == contractId
                }?.name

            // Co-op not found?
            if (coopStatus == null || error != null || contractName == null)
                return NotFound(contractId, coopId)

            // Is co-op abandoned?
            if (coopStatus.contributorsList.isEmpty())
                return Abandoned(coopStatus, contractName)

            val backups: List<EggInc.Backup> = runBlocking(Dispatchers.IO) {
                coopStatus.contributorsList.asyncMap { AuxBrain.getFarmerBackup(it.userId) }
            }.mapNotNull {
                it.component1()
            }

            val contract = backups.findContract(contractId)!!

            // Has the co-op failed?
            if (coopStatus.secondsRemaining < 0.0 && coopStatus.totalAmount.toBigDecimal() < contract.finalGoal)
                return Failed(coopStatus, contractName)

            //
            // Co-op finished?
            //
            // The amount of eggs laid according to the co-op status is higher than the final goal
            // or
            // There is no active farm with this contract
            // and the contract archive contains this contract
            // and that contract has reached its final goal
            // for any of the contributors
            if (coopStatus.eggsLaid >= contract.finalGoal || backups.any { contributor ->
                    contributor.farmsList.none { farm ->
                        farm.contractId == coopStatus.contractId
                    } && contributor.contracts.archiveList.find { contract ->
                        contract.contract.id == coopStatus.contractId
                    }?.finished == true
                }
            ) return Finished(coopStatus, contractName)

            // Co-op in progress
            return InProgress(CoopContractSimulation(backups, coopStatus)).also { it.simulation.run() }
        }
    }
}
