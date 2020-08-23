package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import com.auxbrain.ei.CoopStatusResponse
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.*


fun simulateCoopContract(
    backups: List<Backup>,
    contractId: String,
    coopStatus: CoopStatusResponse,
    catchUp: Boolean = true,
): CoopContractState {
    val localContract: LocalContract? = backups.findContract(contractId, coopStatus.creatorId)
    requireNotNull(localContract) { "Local contract information not found" }
    requireNotNull(localContract.contract) { "Contract information not found" }

    val farmers = backups.mapNotNull { backup -> Farmer(backup, contractId, catchUp) }
    val contractState = CoopContractState(
        localContract, coopStatus.public, farmers
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
            eggspected = when {
                contract.elapsed < contract.timeRemaining ->
                    contract.farmers.sumByBigDecimal { farmer -> farmer.finalState.eggsLaid }
                else -> contract.eggspected
            },
            elapsed = contract.elapsed + ONE_MINUTE
        )
    )
}
