package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.util.*

class CoopContractSimulation private constructor(
    backups: List<EggInc.Backup>,
    val coopStatus: EggInc.CoopStatusResponse
) {

    // region Initialisation

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

    // endregion

    // region Basic info

    val contractId: String by lazy { localContract.contract.id }
    val contractName: String by lazy { localContract.contract.name }
    val coopId: String by lazy { localContract.coopId }
    val egg: EggInc.Egg by lazy { localContract.contract.egg }
    val maxCoopSize by lazy { localContract.contract.maxCoopSize }

    // endregion

    // region Totals

    val eggsLaid: BigDecimal by lazy { farms.sumBy { farm -> farm.eggsLaid } }
    val population: BigDecimal by lazy { farms.sumBy { farm -> farm.population } }
    val populationIncreaseRatePerMinute: BigDecimal by lazy { farms.sumBy { farm -> farm.populationIncreasePerMinute } }
    val populationIncreaseRatePerHour: BigDecimal by lazy { farms.sumBy { farm -> farm.populationIncreasePerHour } }
    val eggLayingRatePerHour: BigDecimal by lazy { farms.sumBy { farm -> farm.eggsLaidPerHour } }

    // endregion

    // region Contract details

    val timeRemaining: Duration by lazy { coopStatus.secondsRemaining.toDuration() }
    val goals: SortedMap<Int, BigDecimal> by lazy {
        localContract.contract.goalsList
            .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
            .toMap()
            .toSortedMap()
    }

    private val completedSimulation by lazy { runSimulation() }

    fun willFinish(): Boolean = completedSimulation.goalsReached.none { (_, duration) -> duration == null }

    fun timeToFinalGoal(): Duration = completedSimulation.goalsReached.maxBy { it.key }?.value ?: oneYear

    // endregion

    // region Simulation

    inner class State(
        private val states: List<ContractSimulation.State> = farms.map { farm -> farm.State() },
        var duration: Duration = Duration.ZERO,
        val goalsReached: MutableMap<Int, Duration?> = goals.map { (i, _) -> i to null }.toMap().toMutableMap(),
        var currentGoal: Int = 0
    ) {
        private val eggsLaid get() = states.sumBy { it.eggsLaid }

        fun step(): Unit = if (eggsLaid >= goals[currentGoal]) {
            goalsReached[currentGoal] = duration
            currentGoal += 1
        } else {
            duration += Duration.standardMinutes(1)
            states.forEach { it.step() }
        }
    }

    private val oneYear: Duration = Duration(DateTime.now(), DateTime.now().plusYears(1))

    fun runSimulation(): State {
        val state = State()
        do {
            state.step()
        } while (state.goalsReached.any { it.value == null } && state.duration < oneYear)
        return state
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
            if (coopStatus == null || error != null) return NotFound(contractId, coopId)

            // Is co-op abandoned?
            if (coopStatus.contributorsList.isEmpty()) return Abandoned(coopStatus, contractName!!)

            val backups: List<EggInc.Backup> = runBlocking(Dispatchers.IO) {
                coopStatus.contributorsList.asyncMap { AuxBrain.getFarmerBackup(it.userId) }
            }.mapNotNull {
                it.component1()
            }

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
