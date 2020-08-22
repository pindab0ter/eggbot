package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.*


fun simulateSoloContract(
    backup: Backup,
    contractId: String,
    catchUp: Boolean = true,
): SoloContractState {
    val localContract: LocalContract? = backup.contracts?.contracts?.find { contract ->
        contract.contract?.id == contractId
    }
    val farm = backup.farmFor(contractId)

    requireNotNull(localContract) { "Local contract information not found" }
    requireNotNull(localContract.contract) { "Contract information not found" }
    requireNotNull(farm) { "Farm not found" }

    val constants = Constants(backup, farm)
    val reportedState = FarmState(farm, constants)
    val farmer = when (catchUp) {
        true -> catchUp(reportedState, minOf(backup.timeSinceBackup, constants.maxAwayTime)).let { adjustedState ->
            Farmer(backup.userName, adjustedState, adjustedState, backup.timeSinceBackup)
        }
        false -> Farmer(backup.userName, reportedState, reportedState, backup.timeSinceBackup)
    }

    val contractState = SoloContractState(
        contractId = localContract.contract.id,
        contractName = localContract.contract.name,
        egg = localContract.contract.egg,
        goals = Goal.fromContract(localContract, farm.eggsLaid),
        timeRemaining = localContract.timeRemaining,
        farmer = farmer
    )

    return simulate(contractState)
}

private tailrec fun simulate(
    contract: SoloContractState,
): SoloContractState = when {
    contract.elapsed >= ONE_YEAR -> contract
    contract.goals.all { goal -> goal.moment != null } -> contract
    else -> simulate(
        contract.copy(
            farmer = contract.farmer.copy(
                finalState = advanceOneMinute(contract.farmer.finalState, contract.elapsed)
            ),
            goals = when {
                contract.goals
                    .filter { it.moment != null }
                    .none { contract.farmer.finalState.eggsLaid >= it.target } -> contract.goals
                else -> contract.goals.map { goal ->
                    if (goal.moment == null && contract.farmer.finalState.eggsLaid >= goal.target) {
                        goal.copy(moment = contract.elapsed)
                    } else goal
                }
            },
            eggspected = when {
                contract.elapsed < contract.timeRemaining -> contract.farmer.finalState.eggsLaid
                else -> contract.eggspected
            },
            elapsed = contract.elapsed + ONE_MINUTE
        )
    )
}
