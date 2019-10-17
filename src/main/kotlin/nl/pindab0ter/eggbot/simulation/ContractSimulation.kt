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

    val log = KotlinLogging.logger { }

    override val farm: EggInc.Backup.Simulation = backup.farmsList.find { it.contractId == localContract.contract.id }!!

    //
    // Basic info
    //

    val contractId: String = localContract.contract.id
    val contractName: String = localContract.contract.name
    val egg: EggInc.Egg = localContract.contract.egg
    var isActive: Boolean = true
    val finished: Boolean = localContract.finished

    //
    // Farm details
    //

    override val population: BigDecimal = farm.habPopulation.sum()
    override val eggsLaid: BigDecimal = farm.eggsLaid.toBigDecimal()

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

    // // TODO: Determine which bottlenecks have been reached
    // fun projectedTimeToGoals(): SortedMap<Int, Duration?>? = if (population == ZERO) null else {
    //     var duration = Duration.ZERO
    //     var projectedPopulation: BigDecimal = population
    //     var projectedEggsLaid: BigDecimal = eggsLaid
    //     val projectedEggLayingRatePerMinute: BigDecimal =
    //         minOf(eggLayingRatePerChickenPerSecond, shippingRatePerMinute) * projectedPopulation * 60
    //     var momentShippingRateReached: Duration? =
    //         if (shippingRatePerMinute <= eggLayingRatePerMinute) Duration.ZERO else null
    //     val goalsReached = sortedMapOf<Int, Duration?>()
    //     var goal = goals[0]
    //
    //     do {
    //         projectedEggsLaid += projectedEggLayingRatePerMinute
    //         if (projectedPopulation < habsMaxCapacity) projectedPopulation =
    //             (projectedPopulation + populationIncreasePerMinute).coerceAtMost(habsMaxCapacity)
    //         if (shippingRatePerMinute <= eggLayingRatePerMinute) momentShippingRateReached = duration
    //         duration += Duration.standardMinutes(1)
    //     } while (projectedEggsLaid < goal)
    //
    //     duration
    // }

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

    // fun runSimulation(): SortedMap<Int, Pair<BigDecimal, Duration?>> = simulationStep().map { (i, duration) ->
    //     Pair(i, Pair(goals[i]!!, duration))
    // }.toMap().toSortedMap()

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
        log.debug { "We're done…" }
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

// // TODO: Determine when bottlenecks have been reached
// private fun simulationStep(): SortedMap<Int, Duration?> {
//     var duration: Duration = Duration.ZERO
//     var projectedPopulation: BigDecimal = population
//     var projectedEggsLaid: BigDecimal = eggsLaid
//     val goalsReached: MutableMap<Int, Duration?> = goals
//         .map { (i, amount) -> Pair(i, if (amount < eggsLaid) Duration.ZERO else null) }
//         .toMap().toMutableMap()
//     var currentGoalNumber = goalsReached.count { (_, duration) -> duration != null }
//
//     while (true) {
//         if ((currentGoalNumber >= goals.size) || (duration > oneYear)) {
//             log.debug { "All goals reached!" }
//             return goalsReached.toSortedMap()
//         }
//
//         duration += Duration.standardMinutes(1)
//         projectedPopulation += minOf(habsMaxCapacity, (projectedPopulation + populationIncreasePerMinute))
//         projectedEggsLaid += minOf(eggsLaidPerChickenPerMinute * projectedPopulation, shippingRatePerMinute)
//         if (projectedEggsLaid > goals[currentGoalNumber]) {
//             goalsReached[currentGoalNumber] = duration
//             currentGoalNumber++
//         }
//     }
// }

// // TODO: Determine when bottlenecks have been reached
// private fun simulationStep(
//     duration: Duration = Duration.ZERO,
//     projectedPopulation: BigDecimal = population,
//     projectedEggsLaid: BigDecimal = eggsLaid,
//     goalsReached: Map<Int, Duration?> = goals
//         .map { (i, amount) -> Pair(i, if (amount < eggsLaid) Duration.ZERO else null) }
//         .toMap()
// ): SortedMap<Int, Duration?> {
//     if (goalsReached.filter { (_, duration) -> duration == null }.size == goals.size
//         || duration > oneYear
//     ) return goalsReached.toSortedMap()
//
//     val projectedEggLayingRatePerMinute: BigDecimal =
//         minOf(eggsLaidPerChickenPerMinute * projectedPopulation, shippingRatePerMinute)
//     val currentGoalNumber: Int =
//         goalsReached.map { it.key }.max() ?: 0
//     val nextGoalsReached =
//         if (projectedEggsLaid > goals[currentGoalNumber]) goalsReached.plus(Pair(currentGoalNumber, duration))
//         else goalsReached
//
//     return simulationStep(
//         duration.plus(Duration.standardMinutes(1)),
//         projectedPopulation.plus(minOf(habsMaxCapacity, (projectedPopulation + populationIncreasePerMinute))),
//         projectedEggsLaid.plus(projectedEggLayingRatePerMinute),
//         nextGoalsReached
//     )
// }

// fun projectedTimeTo(goal: BigDecimal): Duration? = if (population == ZERO) null else {
//     var duration = Duration.ZERO
//     var projectedPopulation: BigDecimal = population
//     var projectedEggsLaid: BigDecimal = eggsLaid
//     val projectedEggLayingRatePerMinute: BigDecimal =
//         minOf(eggLayingRatePerChickenPerSecond, shippingRatePerMinute) * projectedPopulation * 60
//     var momentShippingRateReached: Duration? =
//         if (shippingRatePerMinute <= eggLayingRatePerMinute) Duration.ZERO else null
//
//     do {
//         projectedEggsLaid += projectedEggLayingRatePerMinute
//         if (projectedPopulation < habsMaxCapacity) projectedPopulation =
//             (population + populationIncreaseRatePerMinute).coerceAtMost(habsMaxCapacity)
//         if (shippingRatePerMinute <= eggLayingRatePerMinute) momentShippingRateReached = duration
//         duration += Duration.standardMinutes(1)
//     } while (projectedEggsLaid < goal)
//
//     duration
// }

// fun projectedTimeToFinalGoal(): Duration? = projectedTimeTo(finalGoal)

// fun projectedToFinish(): Boolean = projectedTimeToFinalGoal()?.let { it < timeRemaining } == true

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
