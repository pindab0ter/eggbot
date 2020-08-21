package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.*
import org.joda.time.DateTime
import org.joda.time.Duration


fun simulateCoopContract(
    backups: List<Backup>,
    contractId: String,
    catchUp: Boolean = true,
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
