package nl.pindab0ter.eggbot.model.simulation

import nl.pindab0ter.eggbot.helpers.ONE_MINUTE
import nl.pindab0ter.eggbot.helpers.ONE_YEAR
import nl.pindab0ter.eggbot.helpers.advanceOneMinute

fun simulate(contract: SoloContractState): List<SoloContractState> = simulate(contract, emptyList()).second

tailrec fun simulate(
    state: SoloContractState,
    series: List<SoloContractState>,
): Pair<SoloContractState, List<SoloContractState>> {
    return when {
        state.farmer.timeUpState != null && state.willFinish || state.timeElapsed >= ONE_YEAR -> state to series
        else -> {
            val nextState = state.copy(
                farmer = state.farmer.copy(
                    runningState = advanceOneMinute(
                        state.farmer.runningState,
                        state.farmer.constants,
                        state.timeElapsed
                    ),
                    goalsReachedState = when {
                        state.farmer.goalsReachedState == null && state.willFinish -> state.farmer.runningState
                        else -> state.farmer.goalsReachedState
                    },
                    timeUpState = when {
                        state.farmer.timeUpState == null && state.timeElapsed >= state.timeRemaining -> state.farmer.runningState
                        else -> state.farmer.timeUpState
                    }
                ),
                goals = state.goals.map { goal ->
                    when {
                        goal.moment == null && state.farmer.runningState.eggsLaid >= goal.amount -> {
                            goal.copy(moment = state.timeElapsed)
                        }
                        else -> goal
                    }
                }.toSet(),
                timeElapsed = state.timeElapsed + ONE_MINUTE
            )

            simulate(
                nextState,
                if (nextState.timeElapsed.standardSeconds % 3600L == 0L) series.plus(nextState) else series
            )
        }
    }
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
