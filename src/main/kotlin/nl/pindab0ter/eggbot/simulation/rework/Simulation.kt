package nl.pindab0ter.eggbot.simulation.rework

import com.auxbrain.ei.Backup
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.advanceOneMinute
import nl.pindab0ter.eggbot.model.simulation.*
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.DateTime
import org.joda.time.Duration

fun simulateSoloContract(
    backup: Backup,
    contractId: String,
    catchUp: Boolean = true
): SoloContractState {
    val localContract: LocalContract? = backup.contracts?.contracts?.find { contract ->
        contract.contract?.id == contractId
    }
    val farm = backup.farmFor(contractId)

    requireNotNull(localContract) { "Local contract information not found" }
    requireNotNull(localContract.contract) { "Contract information not found" }
    requireNotNull(farm) { "Farm not found" }

    val constants = Constants(backup, farm)
    val initialState = FarmState(farm, constants)
    val timeSinceLastBackup = Duration(backup.approxTime.toDateTime(), DateTime.now())
    val goals = Goal.fromContract(localContract, farm.eggsLaid)
    val farmer = when {
        catchUp -> catchUp(initialState, timeSinceLastBackup).let { caughtUpState ->
            Farmer(backup.userName, caughtUpState, caughtUpState)
        }
        else -> Farmer(backup.userName, initialState, initialState)
    }
    val contractState = SoloContractState(
        contractId = localContract.contract.id,
        contractName = localContract.contract.name,
        goals = goals,
        timeRemaining = Duration(DateTime.now(), localContract.coopSharedEndTime.toDateTime()),
        farmer = farmer
    )

    return simulate(contractState)
}

fun simulateCoopContract(
    backups: List<Backup>,
    contractId: String,
    catchUp: Boolean = true
): CoopContractState {
    val localContract: LocalContract? = backups.map { backup ->
        backup.contracts?.contracts?.find { contract ->
            contract.contract?.id == contractId
        }
    }.first()

    requireNotNull(localContract) { "Local contract information not found" }
    requireNotNull(localContract.contract) { "Contract information not found" }

    val farms: List<Backup.Simulation> = backups.mapNotNull { backup -> backup.farmFor(contractId) }
    val farmers = backups.mapNotNull farmers@{ backup ->
        val farm: Backup.Simulation = backup.farmFor(contractId) ?: return@farmers null
        val constants = Constants(backup, farm)
        val initialState = FarmState(farm, constants)
        val timeSinceLastBackup = Duration(backup.approxTime.toDateTime(), DateTime.now())

        when {
            catchUp -> catchUp(initialState, timeSinceLastBackup).let { caughtUpState ->
                Farmer(backup.userName, caughtUpState, caughtUpState)
            }
            else -> Farmer(backup.userName, initialState, initialState)
        }
    }
    val contractState = CoopContractState(
        contractId = localContract.contract.id,
        contractName = localContract.contract.name,
        coopId = localContract.coopId,
        goals = Goal.fromContract(localContract, farms.sumByDouble(Backup.Simulation::eggsLaid)),
        timeRemaining = Duration(DateTime.now(), localContract.coopSharedEndTime.toDateTime()),
        farmers = farmers
    )

    return simulate(contractState)
}

private tailrec fun catchUp(
    state: FarmState,
    timeSinceLastBackup: Duration
): FarmState = when {
    timeSinceLastBackup <= Duration.ZERO -> state
    else -> catchUp(
        state = advanceOneMinute(state),
        timeSinceLastBackup = timeSinceLastBackup - ONE_MINUTE
    )
}

private tailrec fun simulate(
    contractState: SoloContractState,
): SoloContractState = when {
    contractState.elapsed >= ONE_YEAR -> contractState
    contractState.goals.all { goal -> goal.moment != null } -> contractState
    else -> simulate(
        contractState.copy(
            farmer = contractState.farmer.copy(
                finalState = advanceOneMinute(contractState.farmer.finalState, contractState.elapsed)
            ),
            goals = when {
                contractState.goals
                    .filter { it.moment != null }
                    .none { contractState.farmer.finalState.eggsLaid >= it.target } -> contractState.goals
                else -> contractState.goals.map { goal ->
                    if (goal.moment == null && contractState.farmer.finalState.eggsLaid >= goal.target) {
                        goal.copy(moment = contractState.elapsed)
                    } else goal
                }
            },
            elapsed = contractState.elapsed + ONE_MINUTE
        )
    )
}

private tailrec fun simulate(
    contractState: CoopContractState,
): CoopContractState = when {
    contractState.elapsed >= ONE_YEAR -> contractState
    contractState.goals.all { goal -> goal.moment != null } -> contractState
    else -> simulate(
        contractState.copy(
            farmers = contractState.farmers.map { farmer ->
                farmer.copy(
                    finalState = advanceOneMinute(farmer.finalState, contractState.elapsed)
                )
            },
            goals = when {
                contractState.goals.filter { it.moment != null }
                    .none { contractState.eggsLaid >= it.target } -> contractState.goals
                else -> contractState.goals.map { goal ->
                    if (goal.moment == null && contractState.farmers.sumByBigDecimal { it.finalState.eggsLaid } >= goal.target) {
                        goal.copy(moment = contractState.elapsed)
                    } else goal
                }
            },
            elapsed = contractState.elapsed + ONE_MINUTE
        )
    )
}

fun main() {
    val coopStatus = AuxBrain.getCoopStatus(contractId = "heat-wave-2020", coopId = "jaj")
    val backups = coopStatus?.contributors?.map { contributor ->
        AuxBrain.getFarmerBackup(contributor.userId)!!
    }!!

    simulateCoopContract(backups, coopStatus.contractId, catchUp = true).apply {
        println(this)
        farmers.forEach(::println)
        println("Before=${farmers.sumByBigDecimal { it.initialState.eggsLaid }.asIllions()}")
        println("Goal=${goals.last().target.asIllions()}")
        println("After=${farmers.sumByBigDecimal { it.finalState.eggsLaid }.asIllions()}")
    }

    simulateCoopContract(backups, coopStatus.contractId, catchUp = false).apply {
        println(this)
        farmers.forEach(::println)
        println("Before=${farmers.sumByBigDecimal { it.initialState.eggsLaid }.asIllions()}")
        println("Goal=${goals.last().target.asIllions()}")
        println("After=${farmers.sumByBigDecimal { it.finalState.eggsLaid }.asIllions()}")
    }

    val backup = AuxBrain.getFarmerBackup("G:1058142539")!!
    simulateSoloContract(backup, "long-days-fwork").apply {
        println(this)
        println(farmer)
        println("Before=${farmer.initialState.eggsLaid.asIllions()}")
        println("Goal=${goals.last().target.asIllions()}")
        println("After=${farmer.finalState.eggsLaid.asIllions()}")
    }
}
