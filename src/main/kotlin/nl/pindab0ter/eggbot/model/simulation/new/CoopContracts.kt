package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.ONE_MINUTE
import nl.pindab0ter.eggbot.helpers.ONE_YEAR
import nl.pindab0ter.eggbot.helpers.advanceOneMinute


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
        val reportedState = FarmState(farm, constants)

        when (catchUp) {
            true -> catchUp(reportedState, minOf(backup.timeSinceBackup, constants.maxAwayTime)).let { adjustedState ->
                Farmer(backup.userName, adjustedState, adjustedState, backup.timeSinceBackup)
            }
            false -> Farmer(backup.userName, reportedState, reportedState, backup.timeSinceBackup)
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
    contract: CoopContractState,
): CoopContractState = when {
    contract.elapsed >= minOf(contract.timeRemaining, ONE_YEAR) -> contract
    else -> simulate(
        contract.copy(
            farmers = contract.farmers.map { farmer ->
                farmer.copy(finalState = advanceOneMinute(farmer.finalState, farmer.constants, contract.elapsed))
            },
            goals = when {
                contract.goals
                    .filter { (_, moment) -> moment == null }
                    .none { (target, _) -> contract.eggsLaid >= target } -> contract.goals
                else -> contract.goals.map { goal ->
                    if (goal.moment == null && contract.eggsLaid >= goal.target) {
                        goal.copy(moment = contract.elapsed)
                    } else goal
                }
            },
            elapsed = contract.elapsed + ONE_MINUTE
        )
    )
}
