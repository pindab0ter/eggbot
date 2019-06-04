package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.sqrt
import nl.pindab0ter.eggbot.sumBy
import nl.pindab0ter.eggbot.toDateTime
import nl.pindab0ter.eggbot.toDuration
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.MathContext.DECIMAL64
import java.util.*

class CoopContractSimulation constructor(
    backups: List<EggInc.Backup>,
    val coopStatus: EggInc.CoopStatusResponse
) {
    val localContract = backups.maxBy { it.approxTime }!!.contracts.contractsList.find { localContract ->
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

    val timeRemaining: Duration = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)

    val goals: SortedMap<Int, BigDecimal> = localContract.contract.goalsList
        .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
        .toMap()
        .toSortedMap()

    val finalGoal: BigDecimal = goals[goals.lastKey()]!!


    //
    //  Projection
    //

    fun timeRequired(goal: BigDecimal): Duration =
        (goal - eggsLaid).coerceAtLeast(ZERO)
            .divide(eggLayingRatePerSecond, DECIMAL64).toLong().toDuration()


    val accelerationFactor: BigDecimal by lazy {
        (eggLayingRatePerSecond * (population + populationIncreaseRatePerSecond)
            .divide(population, DECIMAL64) - eggLayingRatePerSecond).divide(ONE, DECIMAL64)
    }

    // TODO: Take bottlenecks into account
    fun projectedTimeRequired(goal: BigDecimal): Duration =
        (eggLayingRatePerSecond * BigDecimal(-1L) + sqrt(
            eggLayingRatePerSecond.pow(2) + BigDecimal(2) * accelerationFactor * (goal - eggsLaid).coerceAtLeast(ZERO)
        )).divide(accelerationFactor, DECIMAL64).toLong().toDuration()


    companion object {
        operator fun invoke(
            coopStatus: EggInc.CoopStatusResponse
        ): CoopContractSimulation? = runBlocking {
            CoopContractSimulation(coopStatus.contributorsList.map {
                GlobalScope.async { AuxBrain.getFarmerBackup(it.userId) }
            }.map { it.await().get() }, coopStatus)
        }
    }
}
