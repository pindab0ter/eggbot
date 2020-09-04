package nl.pindab0ter.eggbot.model.simulation.old

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatusResponse
import com.auxbrain.ei.Egg
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.simulation.old.CoopContractSimulationResult.*
import org.joda.time.Duration
import java.math.BigDecimal
import java.util.*

class CoopContractSimulation private constructor(
    val backups: List<Backup>,
    val coopStatus: CoopStatusResponse,
) {
    val log = KotlinLogging.logger { }

    // region Basic info

    private val contract: Contract =
        backups.findContract(coopStatus.contractId, coopStatus.creatorId)!!

    val farms: List<ContractSimulation> = backups.filter { backup ->
        backup.farms.any { farm -> farm.contractId == coopStatus.contractId }
    }.mapNotNull { backup ->
        ContractSimulation(backup, contract.id).also {
            // TODO: Save this information somewhere else
            it?.isActive = coopStatus.contributors.find { contributor ->
                contributor.userId == backup.userId
            }?.active == true
        }
    }
    val contractId: String get() = contract.id
    val contractName: String get() = contract.name
    val coopId: String get() = coopStatus.coopId
    val egg: Egg get() = contract.egg
    val maxCoopSize: Int get() = contract.maxCoopSize
    val timeRemaining: Duration get() = coopStatus.secondsRemaining.toDuration()
    val goals: SortedSet<BigDecimal> = contract.goals.map { goal ->
        goal.targetAmount.toBigDecimal()
    }.toSortedSet()
    val tokensAvailable: Int = farms.sumBy { it.farm.boostTokensReceived - it.farm.boostTokensGiven - it.farm.boostTokensSpent }
    val tokensSpent: Int = farms.sumBy { it.farm.boostTokensSpent }

    // endregion Basic info

    // region Simulation

    private var elapsed: Duration = Duration.ZERO
    val currentEggs: BigDecimal = farms.sumByBigDecimal { it.currentEggs }
    private val projectedEggs: BigDecimal get() = farms.sumByBigDecimal { it.projectedEggs }
    val currentPopulation: BigDecimal = farms.sumByBigDecimal { it.currentPopulation }
    lateinit var eggspected: BigDecimal
    val goalReachedMoments: SortedSet<GoalReachedMoment> = goals.map { goal ->
        GoalReachedMoment(goal, if (currentEggs >= goal) Duration.ZERO else null)
    }.toSortedSet()
    private val currentGoal: GoalReachedMoment?
        get() = goalReachedMoments.filter { it.moment == null }.minByOrNull { it.target }
    val willFinish: Boolean
        get() = goalReachedMoments.maxByOrNull { it.target }?.moment?.let { it < timeRemaining } == true
    val goalsReached: Int
        get() = goalReachedMoments.count { (_, moment) -> moment?.let { it < timeRemaining } == true }
    val populationIncreasePerHour: BigDecimal
        get() = farms.sumByBigDecimal { it.populationIncreasePerHour }
    val eggsPerHour: BigDecimal
        get() = farms.sumByBigDecimal { it.currentEggsPerHour }

    private fun step() {
        farms.forEach { it.step() }
        elapsed += Duration.standardMinutes(1)
        if (currentGoal != null && projectedEggs >= currentGoal!!.target)
            currentGoal!!.moment = elapsed
        if (!this::eggspected.isInitialized && elapsed >= timeRemaining)
            eggspected = projectedEggs
    }

    fun run() {
        do step() while (
            elapsed <= timeRemaining ||                                            // Time limit hasn't been reached or
            (goalReachedMoments.any { it.moment == null } && elapsed <= ONE_YEAR) // the goals haven't been reached and a year hasn't yet passed
        )
    }

    // endregion

    companion object Factory {

        val log = KotlinLogging.logger { }

        operator fun invoke(
            contract: Contract,
            coopId: String,
            message: Message? = null,
        ): CoopContractSimulationResult {
            val coopStatus = AuxBrain.getCoopStatus(contract.id, coopId) ?: return NotFound(contract.id, coopId)

            // Is co-op abandoned?
            if (coopStatus.contributors.isEmpty())
                return Abandoned(coopStatus, contract.name)

            message?.editMessage("Fetching backups…")?.complete()
            message?.channel?.sendTyping()?.complete()

            val backups: List<Backup> = coopStatus.contributors.parallelMap { contributor ->
                AuxBrain.getFarmerBackup(contributor.userId)
            }.filterNotNull()

            // Has the co-op failed?
            if (coopStatus.secondsRemaining < 0.0 && coopStatus.totalAmount.toBigDecimal() < contract.finalGoal)
                return Failed(coopStatus, contract.name)

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
                    contributor.farms.none { farm ->
                        farm.contractId == coopStatus.contractId
                    } && contributor.contracts!!.archive.find { contract ->
                        contract.contract!!.id == coopStatus.contractId
                    }?.finished == true
                }
            ) return Finished(coopStatus, contract.name)

            // Co-op in progress
            return InProgress(CoopContractSimulation(backups, coopStatus)).also {
                message?.editMessage("Running simulation…")?.queue()
                message?.channel?.sendTyping()?.queue()
                it.simulation.run()
            }
        }
    }
}