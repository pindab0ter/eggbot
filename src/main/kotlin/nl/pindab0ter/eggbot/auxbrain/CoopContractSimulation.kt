package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.auxbrain.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.network.AuxBrain
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
    val localContract: EggInc.LocalContract = backups.filter { backup ->
        backup.farmsList.any { it.contractId == coopStatus.contractIdentifier }
    }.maxBy { it.approxTime }!!.contracts.contractsList.find { localContract ->
        localContract.contract.identifier == coopStatus.contractIdentifier
    }!!

    val farms: List<ContractSimulation> = backups.map { backup ->
        ContractSimulation(backup, localContract).also {
            it.isActive = coopStatus.contributorsList.find { contributor ->
                contributor.userId == backup.userid
            }!!.active == 1
        }
    }

    //
    // Basic info
    //

    val contractId: String = localContract.contract.identifier
    val contractName: String = localContract.contract.name
    val coopId: String = localContract.coopIdentifier
    val egg: EggInc.Egg = localContract.contract.egg
    val maxCoopSize = localContract.contract.maxCoopSize

    //
    // Totals
    //

    val eggsLaid: BigDecimal = farms.sumBy { farm -> farm.eggsLaid }
    val eggLayingRatePerSecond: BigDecimal = farms.sumBy { farm -> farm.eggLayingRatePerSecond }
    val population: BigDecimal = farms.sumBy { farm -> farm.population }
    val populationIncreaseRatePerSecond: BigDecimal = farms.sumBy { farm -> farm.populationIncreaseRatePerSecond }
    val populationIncreaseRatePerMinute: BigDecimal = farms.sumBy { farm -> farm.populationIncreaseRatePerMinute }
    val populationIncreaseRatePerHour: BigDecimal = farms.sumBy { farm -> farm.populationIncreaseRatePerHour }
    val eggLayingRatePerMinute: BigDecimal = farms.sumBy { farm -> farm.eggLayingRatePerMinute }
    val eggLayingRatePerHour: BigDecimal = farms.sumBy { farm -> farm.eggLayingRatePerMinute }
    val currentEggLayingRatePerSecond: BigDecimal = farms.sumBy { farm -> farm.currentEggLayingRatePerSecond }
    val currentEggLayingRatePerMinute: BigDecimal = farms.sumBy { farm -> farm.currentEggLayingRatePerMinute }

    //
    // Contract details
    //

    val elapsedTime: Duration = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())

    val timeRemaining: Duration = coopStatus.secondsRemaining.toDuration()

    val goals: SortedMap<Int, BigDecimal> = localContract.contract.goalsList
        .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
        .toMap()
        .toSortedMap()

    val finalGoal: BigDecimal = goals[goals.lastKey()]!!


    //
    //  Projection
    //

    fun timeTo(goal: BigDecimal): Duration =
        (goal - eggsLaid).coerceAtLeast(ZERO)
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

            // Co-op not found?
            if (coopStatus == null || error != null) return NotFound(contractId, coopId)

            // Co-op not empty?
            val backups: List<EggInc.Backup> = runBlocking(Dispatchers.IO) {
                coopStatus.contributorsList.asyncMap { AuxBrain.getFarmerBackup(it.userId) }
            }.mapNotNull {
                it.component1()
            }.takeIf { backups ->
                backups.any { backup -> backup.farmsList.any { it.contractId == coopStatus.contractIdentifier } }
            } ?: return Empty(contractId, coopId)

            // Co-op finished?
            if (backups.any { contributor ->
                    contributor.contracts.archiveList.find { contract ->
                        contract.contract.identifier == coopStatus.contractIdentifier
                    }?.let { contract ->
                        contract.lastAmountWhenRewardGiven >= contract.contract.goalsList.last().targetAmount
                    } == true
                }
            ) return Finished(contractId, coopId)

            // Co-op in progress
            return InProgress(CoopContractSimulation(backups, coopStatus))
        }
    }
}
