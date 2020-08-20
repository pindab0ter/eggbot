package nl.pindab0ter.eggbot.simulation.rework

import com.auxbrain.ei.*
import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import kotlin.system.measureTimeMillis

private val EGG_LAYING_BASE_RATE_PER_MINUTE = BigDecimal.ONE / BigDecimal(30)

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

    val contributors = backups.map { backup ->
        val farm = backup.farmFor(contractId)!!
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
        val initialState = FarmState(
            habs = (0..3).map { index ->
                Hab(
                    population = farm.habPopulation[index].toBigDecimal(),
                    capacity = farm.habs[index].capacity.multiply(farm.habCapacityMultipliers.product())
                )
            },
            eggsLaid = farm.eggsLaid.toBigDecimal(),
            constants = constants
        )
        val timeSinceLastBackup = Duration(backup.approxTime.toDateTime(), DateTime.now())

        Contributor(
            name = backup.userName,
            initialState = if (catchUp) catchUp(initialState, timeSinceLastBackup) else initialState,
            finalState = if (catchUp) catchUp(initialState, timeSinceLastBackup) else initialState,
        )
    }

    val contractState = ContractState(
        id = localContract.contract.id,
        name = localContract.contract.name,
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
        state = state.advanceOneMinute(),
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
                    finalState = contributor.finalState.advanceOneMinute(state.elapsed)
                )
            },
            // TODO: Skip if no new goals reached
            goals = state.goals.map { goal ->
                if (goal.moment == null && state.contributors.sumByBigDecimal { it.finalState.eggsLaid } >= goal.target) {
                    goal.copy(moment = state.elapsed)
                } else goal
            },
            elapsed = state.elapsed + ONE_MINUTE
        )
    )
}

private fun FarmState.advanceOneMinute(elapsed: Duration = Duration.ZERO): FarmState = copy(
    eggsLaid = eggsLaid + minOf(
        habs.sumByBigDecimal(Hab::population)
            .multiply(EGG_LAYING_BASE_RATE_PER_MINUTE)
            .multiply(constants.eggLayingBonus),
        constants.transportRate
    ),
    habs = habs.map { hab ->
        val internalHatcherySharingMultiplier = habs.fold(BigDecimal.ONE) { acc, (population, capacity) ->
            if (population == capacity) acc + BigDecimal.ONE else acc
        }.multiply(constants.internalHatcherySharing)

        hab.copy(
            population = minOf(
                hab.population + constants.internalHatcheryRate.multiply(internalHatcherySharingMultiplier),
                hab.capacity
            )
        )
    },
    habBottleneckReached = habBottleneckReached ?: when {
        habs.all { (population, capacity) -> population == capacity } -> elapsed
        else -> null
    },
    // TODO
    transportBottleneckReached = null
)

data class ContractState(
    val id: String,
    val name: String,
    val goals: List<Goal>,
    val timeRemaining: Duration,
    val elapsed: Duration = Duration.ZERO,
    val contributors: List<Contributor>,
)

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
)

data class FarmState(
    val habs: List<Hab>,
    val eggsLaid: BigDecimal = BigDecimal.ZERO,
    val constants: Constants,
    val habBottleneckReached: Duration? = null,
    val transportBottleneckReached: Duration? = null,
)

fun main() {
    val coopStatus = AuxBrain.getCoopStatus("ion-drive", "trains")
    val backups = coopStatus?.contributors?.map { contributor ->
        AuxBrain.getFarmerBackup(contributor.userId)!!
    }!!

    val result = simulation(backups, coopStatus.contractId, false)

    println(result)
    result.contributors.forEach(::println)
}
