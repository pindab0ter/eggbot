package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import com.auxbrain.ei.CoopStatusResponse
import nl.pindab0ter.eggbot.helpers.*


// TODO: Inline
fun simulateCoopContract(
    backups: List<Backup>,
    contractId: String,
    coopStatus: CoopStatusResponse,
    catchUp: Boolean = true,
): CoopContractState? {
    // TODO: Throw error
    val contract = backups.findContract(contractId, coopStatus.creatorId) ?: return null

    val farmers = backups.mapNotNull { backup -> Farmer(backup, contractId, catchUp) }

    // TODO: Throw error
    if (farmers.isEmpty()) return null

    val contractState = CoopContractState(contract, coopStatus, farmers)

    return simulate(contractState)
}

private tailrec fun simulate(
    contract: CoopContractState,
): CoopContractState = when {
    contract.goals.last().moment != null -> contract
    contract.elapsed >= ONE_YEAR -> contract
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
                contract.elapsed <= contract.timeRemaining ->
                    contract.farmers.sumByBigDecimal { farmer -> farmer.finalState.eggsLaid }
                else -> contract.eggspected
            },
            elapsed = contract.elapsed + ONE_MINUTE
        )
    )
}
