package nl.pindab0ter.eggbot.simulation.rework

import com.auxbrain.ei.*
import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import kotlin.system.measureTimeMillis

/** Base egg laying rate per chicken per minute.
 *
 * A chicken lays 1/30 of an egg per second, so 2 per minute */
private val EGG_LAYING_BASE_RATE = BigDecimal(2)

fun simulation(
    backups: List<Backup>,
    contractId: String,
    catchUp: Boolean = true
): ContractState {
    val localContract: LocalContract? = backups.map { backup ->
        backup.contracts?.contracts?.find { contract ->
            contract.contract?.id == contractId
        }
    }.first()

    requireNotNull(localContract) { "Local contract information not found" }
    requireNotNull(localContract.contract) { "Contract information not found" }

    val farms: List<Backup.Simulation> = backups.mapNotNull { backup -> backup.farmFor(contractId) }

    val contributors = backups.mapNotNull { backup ->
        val farm = backup.farmFor(contractId) ?: return@mapNotNull null

        val constants = Constants(
            internalHatcherySharing = backup.internalHatcherySharing,
            internalHatcheryRate = farm.internalHatcheryFlatIncreases.sum()
                .multiply(backup.internalHatcheryMultiplier),
            habCapacityMultiplier = farm.habCapacityMultipliers.sum(),
            eggLayingBonus = farm.eggLayingCommonResearchMultipliers
                .plus(backup.eggLayingEpicResearchMultiplier)
                .product(),
            transportRate = farm.baseShippingRate.multiply(
                farm.shippingRateCommonResearchMultipliers
                    .plus(backup.shippingRateEpicResearchMultiplier)
                    .product()
            )
        )
        val habs = (0..3).map { index ->
            Hab(
                population = farm.habPopulation[index].toBigDecimal(),
                capacity = farm.habs[index].capacity.multiply(farm.habCapacityMultipliers.product())
            )
        }
        val initialState = FarmState(
            habs = habs,
            eggsLaid = farm.eggsLaid.toBigDecimal(),
            constants = constants,
            habBottleneck = when {
                habs.sumByBigDecimal(Hab::population) >= habs.sumByBigDecimal(Hab::capacity) -> Duration.ZERO
                else -> null
            },
            transportBottleneck = when {
                eggIncrease(habs, constants) >= constants.transportRate -> Duration.ZERO
                else -> null
            }
        )
        val timeSinceLastBackup = Duration(backup.approxTime.toDateTime(), DateTime.now())

        Contributor(
            name = backup.userName,
            initialState = if (catchUp) catchUp(initialState, timeSinceLastBackup) else initialState,
            finalState = if (catchUp) catchUp(initialState, timeSinceLastBackup) else initialState,
        )
    }

    val contractState = ContractState(
        contractId = localContract.contract.id,
        contractName = localContract.contract.name,
        coopId = localContract.coopId,
        goals = localContract.contract.goals.map { goal: Contract.Goal ->
            Goal(
                target = goal.targetAmount.toBigDecimal(),
                moment = if (farms.any { farm -> farm.eggsLaid >= goal.targetAmount }) Duration.ZERO else null
            )
        },
        timeRemaining = Duration(DateTime.now(), localContract.coopSharedEndTime.toDateTime()),
        contributors = contributors
    )


    var result: ContractState

    val timeTaken = measureTimeMillis {
        result = simulate(contractState)
    }

    println("${timeTaken}ms")
    return result
}

private tailrec fun catchUp(
    state: FarmState,
    timeSinceLastBackup: Duration
): FarmState = when {
    timeSinceLastBackup >= Duration.ZERO -> state
    else -> catchUp(
        state = advanceOneMinute(state),
        timeSinceLastBackup = timeSinceLastBackup - ONE_MINUTE
    )
}

private tailrec fun simulate(
    state: ContractState,
): ContractState = when {
    state.elapsed >= ONE_YEAR -> state
    state.goals.all { goal -> goal.moment != null } -> state
    else -> simulate(
        state.copy(
            contributors = state.contributors.map { contributor ->
                contributor.copy(
                    finalState = advanceOneMinute(contributor.finalState, state.elapsed)
                )
            },
            goals = when {
                state.goals.filter { it.moment != null }.none { state.eggsLaid >= it.target } -> state.goals
                else -> state.goals.map { goal ->
                    if (goal.moment == null && state.contributors.sumByBigDecimal { it.finalState.eggsLaid } >= goal.target) {
                        goal.copy(moment = state.elapsed)
                    } else goal
                }
            },
            elapsed = state.elapsed + ONE_MINUTE
        )
    )
}

private fun advanceOneMinute(state: FarmState, elapsed: Duration = Duration.ZERO): FarmState = state.copy(
    eggsLaid = state.eggsLaid + minOf(eggIncrease(state.habs, state.constants), state.constants.transportRate),
    habs = state.habs.map { hab ->
        val internalHatcherySharingMultiplier = state.habs.fold(BigDecimal.ONE) { acc, (population, capacity) ->
            if (population == capacity) acc + BigDecimal.ONE else acc
        }.multiply(state.constants.internalHatcherySharing)
        hab.copy(
            population = minOf(
                hab.population + state.constants.internalHatcheryRate.multiply(internalHatcherySharingMultiplier),
                hab.capacity
            )
        )
    },
    habBottleneck = state.habBottleneck ?: habBottleneck(state.habs, elapsed),
    transportBottleneck = state.transportBottleneck ?: transportBottleneck(state.habs, state.constants, elapsed)
)

private fun transportBottleneck(habs: List<Hab>, constants: Constants, elapsed: Duration): Duration? {
    return when {
        eggIncrease(habs, constants) >= constants.transportRate -> elapsed
        else -> null
    }
}

private fun habBottleneck(habs: List<Hab>, elapsed: Duration): Duration? {
    return when {
        habs.all { (population, capacity) -> population == capacity } -> elapsed
        else -> null
    }
}

private fun eggIncrease(habs: List<Hab>, constants: Constants) =
    habs.sumByBigDecimal(Hab::population).multiply(EGG_LAYING_BASE_RATE).multiply(constants.eggLayingBonus)

data class ContractState(
    val contractId: String,
    val contractName: String,
    val coopId: String,
    val goals: List<Goal>,
    val timeRemaining: Duration,
    val elapsed: Duration = Duration.ZERO,
    val contributors: List<Contributor>,
) {
    val eggsLaid: BigDecimal get() = contributors.sumByBigDecimal { contributor -> contributor.finalState.eggsLaid }

    override fun toString(): String = "${this::class.simpleName}(" +
            "contractId=${contractId}, " +
            "contractName=${contractName}, " +
            "coopId=${coopId}, " +
            "goals=${goals}, " +
            "timeRemaining=${timeRemaining.asDaysHoursAndMinutes()}, " +
            "elapsed=${elapsed.asDaysHoursAndMinutes()}" +
            ")"
}

data class Contributor(
    val name: String,
    val initialState: FarmState, // TODO: Remove?
    val finalState: FarmState,
)

data class Constants(
    val internalHatcherySharing: BigDecimal,
    val internalHatcheryRate: BigDecimal,
    val habCapacityMultiplier: BigDecimal,
    val eggLayingBonus: BigDecimal,
    val transportRate: BigDecimal,
)

data class Hab(
    val population: BigDecimal,
    val capacity: BigDecimal,
)

data class Goal(
    val target: BigDecimal,
    val moment: Duration? = null,
) {
    override fun toString(): String = "${this::class.simpleName}(" +
            "target=${target.asIllions()}, " +
            "moment=${moment?.asDaysHoursAndMinutes()}" +
            ")"
}

data class FarmState(
    val habs: List<Hab>,
    val eggsLaid: BigDecimal = BigDecimal.ZERO,
    val constants: Constants,
    val habBottleneck: Duration? = null,
    val transportBottleneck: Duration? = null,
) {
    override fun toString(): String = "${this::class.simpleName}(" +
            "population=${habs.sumByBigDecimal(Hab::population).asIllions()}, " +
            "capacity=${habs.sumByBigDecimal(Hab::capacity).asIllions()}, " +
            "eggsLaid=${eggsLaid.asIllions()}, " +
            "habBottleneckReached=${habBottleneck?.asDaysHoursAndMinutes()}, " +
            "transportBottleneckReached=${transportBottleneck?.asDaysHoursAndMinutes()}" +
            ")"
}

fun main() {
    val coopStatus = AuxBrain.getCoopStatus(contractId = "heat-wave-2020", coopId = "jaj")
    val backups = coopStatus?.contributors?.map { contributor ->
        AuxBrain.getFarmerBackup(contributor.userId)!!
    }!!

    simulation(backups, coopStatus.contractId, catchUp = true).apply {
        println(this)
        contributors.forEach(::println)
        println("Before=${contributors.sumByBigDecimal { it.initialState.eggsLaid }.asIllions()}")
        println("Goal=${goals.last().target.asIllions()}")
        println("After=${contributors.sumByBigDecimal { it.finalState.eggsLaid }.asIllions()}")
    }
}
