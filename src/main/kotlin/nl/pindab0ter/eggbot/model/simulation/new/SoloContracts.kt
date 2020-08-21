package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.*
import org.joda.time.DateTime
import org.joda.time.Duration


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
            Farmer(backup.userName, adjustedState, adjustedState)
        }
        false -> Farmer(backup.userName, reportedState, reportedState)
    }

    val contractState = SoloContractState(
        contractId = localContract.contract.id,
        contractName = localContract.contract.name,
        goals = Goal.fromContract(localContract, farm.eggsLaid),
        timeRemaining = Duration(DateTime.now(), localContract.coopSharedEndTime.toDateTime()),
        farmer = farmer
    )

    return simulate(contractState)
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
