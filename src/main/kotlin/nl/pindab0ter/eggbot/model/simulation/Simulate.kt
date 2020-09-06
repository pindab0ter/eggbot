package nl.pindab0ter.eggbot.model.simulation

import nl.pindab0ter.eggbot.helpers.ONE_MINUTE
import nl.pindab0ter.eggbot.helpers.ONE_YEAR
import nl.pindab0ter.eggbot.helpers.advanceOneMinute
import nl.pindab0ter.eggbot.helpers.sumByBigDecimal


tailrec fun simulate(
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

tailrec fun simulate(
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