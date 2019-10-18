package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import mu.KotlinLogging
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.util.*

class ContractSimulation constructor(
    backup: EggInc.Backup,
    private val localContract: EggInc.LocalContract
) : Simulation(backup) {

    // region Initialisation

    val log = KotlinLogging.logger { }

    override val farm: EggInc.Backup.Simulation = backup.farmsList.find { it.contractId == localContract.contract.id }!!

    // endregion

    // region Basic info

    val contractId: String = localContract.contract.id
    val contractName: String = localContract.contract.name
    val egg: EggInc.Egg = localContract.contract.egg
    var isActive: Boolean = true
    val finished: Boolean = localContract.finished

    // endregion

    // region Farm details

    override val population: BigDecimal = farm.habPopulation.sum()
    override val eggsLaid: BigDecimal = farm.eggsLaid.toBigDecimal()

    // endregion

    // region Contract details

    val elapsedTime: Duration = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())

    val timeRemaining: Duration = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)

    val goals: SortedMap<Int, BigDecimal> = localContract.contract.goalsList
        .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
        .toMap()
        .toSortedMap()

    // endregion

    // region  Simulation State

    open inner class State(
        var population: BigDecimal = this.population,
        var eggsLaid: BigDecimal = this.eggsLaid
    ) {
        open fun step(): Unit {
            population = (population + populationIncreasePerMinute).coerceAtMost(habsMaxCapacity)
            eggsLaid += (eggsLaidPerChickenPerMinute * population).coerceAtMost(shippingRatePerMinute)
        }
    }

    inner class SoloState(
        var duration: Duration = Duration.ZERO,
        population: BigDecimal = this.population,
        eggsLaid: BigDecimal = this.eggsLaid,
        val goalsReached: MutableMap<Int, Duration?> = goals.map { (i, amount) -> i to null }.toMap().toMutableMap(),
        var currentGoal: Int = 0
    ) : State(population, eggsLaid) {
        override fun step(): Unit = if (eggsLaid >= goals[currentGoal]) {
            goalsReached[currentGoal] = duration
            currentGoal += 1
        } else {
            duration += Duration.standardMinutes(1)
            population = (population + populationIncreasePerMinute).coerceAtMost(habsMaxCapacity)
            eggsLaid += (eggsLaidPerChickenPerMinute * population).coerceAtMost(shippingRatePerMinute)
        }
    }

    // endregion

    // region Simulation execution

    private val oneYear: Duration = Duration(DateTime.now(), DateTime.now().plusYears(1))

    fun runSimulation(): SoloState {
        val state = SoloState()
        do {
            state.step()
        } while (state.goalsReached.any { it.value == null } && state.duration < oneYear)
        return state
    }

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
