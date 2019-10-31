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

    // endregion Initialisation

    // region Basic info

    val contractId: String = localContract.contract.id
    val contractName: String = localContract.contract.name
    val egg: EggInc.Egg = localContract.contract.egg
    var isActive: Boolean = true
    val finished: Boolean = localContract.finished

    // endregion Basic info

    // region Farm details

    override val population: BigDecimal = farm.habPopulation.sum()
    override val eggsLaid: BigDecimal = farm.eggsLaid.toBigDecimal()

    // endregion Farm details

    // region Contract details

    val elapsed: Duration = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())

    val timeRemaining: Duration = localContract.contract.lengthSeconds.toDuration().minus(elapsed)

    val goals: SortedMap<Int, BigDecimal> = localContract.contract.goalsList
        .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
        .toMap()
        .toSortedMap()

    // endregion Contract details

    // region Simulation

    val finalState: SoloState by lazy { runSimulation() }

    open inner class State(
        var population: BigDecimal = this.population,
        var eggsLaid: BigDecimal = this.eggsLaid
    ) {
        open fun step() {
            population = (population + populationIncreasePerMinute).coerceAtMost(habsMaxCapacity)
            eggsLaid += (eggsLaidPerChickenPerMinute * population).coerceAtMost(shippingRatePerMinute)
        }
    }

    inner class SoloState(
        var elapsed: Duration = Duration.ZERO,
        population: BigDecimal = this.population,
        eggsLaid: BigDecimal = this.eggsLaid,
        val reachedGoals: MutableMap<GoalNumber, TimeUntilReached?> = goals.map { (i, _) -> i to null }.toMap().toMutableMap(),
        private var currentGoal: Int = 0
    ) : State(population, eggsLaid) {
        override fun step(): Unit = when {
            eggsLaid >= goals[currentGoal] -> {
                reachedGoals[currentGoal] = elapsed
                currentGoal += 1
            }
            else -> {
                elapsed += Duration.standardMinutes(1)
                population = (population + populationIncreasePerMinute).coerceAtMost(habsMaxCapacity)
                eggsLaid += (eggsLaidPerChickenPerMinute * population).coerceAtMost(shippingRatePerMinute)
            }
        }
    }

    private val oneYear: Duration = Duration(DateTime.now(), DateTime.now().plusYears(1))

    private fun runSimulation(): SoloState {
        val state = SoloState()
        do {
            state.step()
        } while (
        // Not all goals have been reached in time
            (state.reachedGoals.any { it.value == null } && state.elapsed < timeRemaining)
            // Or one year hasn't yet passed
            || state.elapsed < oneYear
        )
        return state
    }

    // endregion Simulation

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
