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

    val farmers = backups.mapNotNull { backup -> Farmer(backup, contractId, catchUp) }
    val contractState = CoopContractState(
        localContract, false, farmers
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
            }.toSet(),
            elapsed = contract.elapsed + ONE_MINUTE
        )
    )
}
