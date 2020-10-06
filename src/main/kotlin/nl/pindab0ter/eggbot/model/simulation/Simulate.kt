package nl.pindab0ter.eggbot.model.simulation

import nl.pindab0ter.eggbot.helpers.ONE_MINUTE
import nl.pindab0ter.eggbot.helpers.ONE_YEAR
import nl.pindab0ter.eggbot.helpers.advanceOneMinute


tailrec fun simulate(
    contract: SoloContractState,
): SoloContractState = when {
    (contract.farmer.timeUpState != null && contract.willFinish) || contract.timeElapsed >= ONE_YEAR -> contract
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
                    else -> contract.farmer.goalsReachedState
                },
                timeUpState = when {
                    contract.farmer.timeUpState == null && contract.timeElapsed >= contract.timeRemaining -> contract.farmer.runningState
                    else -> contract.farmer.timeUpState
                }
            ),
            goals = contract.goals.map { goal ->
                when {
                    goal.moment == null && contract.farmer.runningState.eggsLaid >= goal.amount -> {
                        goal.copy(moment = contract.timeElapsed)
                    }
                    else -> goal
                }
            }.toSet(),
            timeElapsed = contract.timeElapsed + ONE_MINUTE
        )
    )
}

tailrec fun simulate(
    contract: CoopContractState,
): CoopContractState = when {
    (contract.farmers.any { farmer -> farmer.timeUpState != null } && contract.willFinish) || contract.timeElapsed >= ONE_YEAR -> contract
    else -> simulate(
        contract.copy(
            farmers = contract.farmers.map { farmer ->
                farmer.copy(
                    runningState = advanceOneMinute(farmer.runningState, farmer.constants, contract.timeElapsed),
                    goalsReachedState = when {
                        farmer.goalsReachedState == null && contract.willFinish -> farmer.runningState
                        else -> farmer.goalsReachedState
                    },
                    timeUpState = when {
                        farmer.timeUpState == null && contract.timeElapsed >= contract.timeRemaining -> farmer.runningState
                        else -> farmer.timeUpState
                    }
                )
            },
            goals = contract.goals.map { goal ->
                when {
                    goal.moment == null && contract.runningEggsLaid >= goal.amount -> {
                        goal.copy(moment = contract.timeElapsed)
                    }
                    else -> goal
                }
            }.toSet(),
            timeElapsed = contract.timeElapsed + ONE_MINUTE
        )
    )
}
