package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Contract
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.ONE_MINUTE
import nl.pindab0ter.eggbot.helpers.ONE_YEAR
import nl.pindab0ter.eggbot.helpers.advanceOneMinute
import nl.pindab0ter.eggbot.helpers.timeRemaining


fun simulateSoloContract(
    backup: Backup,
    contractId: String,
    catchUp: Boolean = true,
): SoloContractState {
    val localContract: LocalContract? = backup.contracts?.contracts?.find { contract ->
        contract.contract?.id == contractId
    }
    val contract: Contract? = localContract?.contract
    val farmer = Farmer(backup, contractId, catchUp)

    // TODO: Are these errors caught and handled?
    requireNotNull(localContract) { "Local contract information not found" }
    requireNotNull(contract) { "Contract information not found" }
    requireNotNull(farmer) { "Farm not found" }

    val contractState = SoloContractState(
        contractId = contract.id,
        contractName = contract.name,
        egg = contract.egg,
        goals = Goal.fromContract(contract, farmer.initialState.eggsLaid),
        timeRemaining = localContract.timeRemaining,
        farmer = farmer
    )

    return simulate(contractState)
}

private tailrec fun simulate(
    contract: SoloContractState,
): SoloContractState = when {
    contract.goals.last().moment != null -> contract
    contract.elapsed >= ONE_YEAR -> contract
    else -> simulate(
        contract.copy(
            farmer = contract.farmer.copy(
                finalState = advanceOneMinute(contract.farmer.finalState, contract.farmer.constants, contract.elapsed)
            ),
            goals = when {
                contract.goals
                    .filter { (_, moment) -> moment == null }
                    .none { (target, _) -> contract.farmer.finalState.eggsLaid >= target } -> contract.goals
                else -> contract.goals.map { goal ->
                    if (goal.moment == null && contract.farmer.finalState.eggsLaid >= goal.target) {
                        goal.copy(moment = contract.elapsed)
                    } else goal
                }
            }.toSet(),
            eggspected = when {
                contract.elapsed <= contract.timeRemaining -> contract.farmer.finalState.eggsLaid
                else -> contract.eggspected
            },
            elapsed = contract.elapsed + ONE_MINUTE
        )
    )
}
