package nl.pindab0ter.eggbot.model.simulation

import nl.pindab0ter.eggbot.helpers.ONE_MINUTE
import nl.pindab0ter.eggbot.helpers.ONE_YEAR
import nl.pindab0ter.eggbot.helpers.advanceOneMinute


tailrec fun simulate(
    contract: SoloContractState,
): SoloContractState = when {
    (contract.timeElapsed >= contract.timeRemaining && contract.willFinish) || contract.timeElapsed >= ONE_YEAR -> contract
    else -> simulate(
        contract.copy(
            farmer = contract.farmer.copy(
                runningState = advanceOneMinute(
                    contract.farmer.runningState,
                    contract.farmer.constants,
                    contract.timeElapsed
                ),
                goalsReachedState = when {
                    contract.farmer.goalsReachedState == null && contract.willFinish -> contract.farmer.runningState
                    else -> null
                }
            ),
            goals = when {
                contract.goals
                    .filter { (_, moment) -> moment == null }
                    .none { (target, _) -> contract.farmer.runningState.eggsLaid >= target } -> contract.goals
                else -> contract.goals.map { goal ->
                    if (goal.moment == null && contract.farmer.runningState.eggsLaid >= goal.target) {
                        goal.copy(moment = contract.timeElapsed)
                    } else goal
                }
            }.toSet(),
            timeElapsed = contract.timeElapsed + ONE_MINUTE
        )
    )
}

tailrec fun simulate(
    contract: CoopContractState,
): CoopContractState = when {
    (contract.timeElapsed >= contract.timeRemaining && contract.willFinish) || contract.timeElapsed >= ONE_YEAR -> contract
    else -> simulate(
        contract.copy(
            farmers = contract.farmers.map { farmer ->
                farmer.copy(
                    runningState = advanceOneMinute(farmer.runningState, farmer.constants, contract.timeElapsed),
                    goalsReachedState = when {
                        farmer.goalsReachedState == null && contract.willFinish -> farmer.runningState
                        else -> null
                    }
                )
            },
            goals = when {
                contract.goals
                    .filter { (_, moment) -> moment == null }
                    .none { (target, _) -> contract.timeUpEggsLaid >= target } -> contract.goals
                else -> contract.goals.map { goal ->
                    if (goal.moment == null && contract.timeUpEggsLaid >= goal.target) {
                        goal.copy(moment = contract.timeElapsed)
                    } else goal
                }
            }.toSet(),
            timeElapsed = contract.timeElapsed + ONE_MINUTE
        )
    )
}
