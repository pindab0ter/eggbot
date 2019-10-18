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


    private val oneYear: Duration = Duration(DateTime.now(), DateTime.now().plusYears(1))

    data class State(
        var duration: Duration,
        var population: BigDecimal,
        var eggsLaid: BigDecimal,
        val goalsReached: Map<Int, Duration?>,
        val currentGoal: Int
    ) {
        override fun toString(): String =
            "SimulationState(duration=${duration.asDayHoursAndMinutes()}, projectedPopulation=${population.formatIllions()}, projectedEggsLaid=${eggsLaid.formatIllions()}, goalsReached=${goalsReached}, currentGoal=$currentGoal)"
    }

    // endregion

    // region Simulation execution

    fun runSimulation(): State {
        var state = State(
            duration = Duration.ZERO,
            population = population,
            eggsLaid = eggsLaid,
            goalsReached = goals.map { (i, amount) ->
                Pair(i, if (amount < eggsLaid) Duration.ZERO else null)
            }.toMap().toSortedMap(),
            currentGoal = 0
        )
        do {
            state = simulationStep(state)
            log.debug { state }
        } while (state.goalsReached.any { it.value == null } && state.duration < oneYear)
        return state
    }

    // TODO: Determine when bottlenecks have been reached
    private fun simulationStep(state: State): State = State(
        state.duration + Duration.standardMinutes(1),
        state.population + minOf(
            habsMaxCapacity,
            (state.population + populationIncreasePerMinute)
        ),
        state.eggsLaid + minOf(
            eggsLaidPerChickenPerMinute * state.population,
            shippingRatePerMinute
        ),
        if (state.eggsLaid > goals[state.currentGoal]) {
            state.goalsReached.minus(state.currentGoal).plus(Pair(state.currentGoal, state.duration))
        } else state.goalsReached,
        if (state.eggsLaid > goals[state.currentGoal]) (state.currentGoal + 1)
        else state.currentGoal
    )


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
