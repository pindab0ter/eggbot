package nl.pindab0ter.eggbot.model.simulation

import nl.pindab0ter.eggbot.helpers.ONE_MINUTE
import nl.pindab0ter.eggbot.helpers.ONE_YEAR
import nl.pindab0ter.eggbot.helpers.advanceOneMinute

fun simulate(contract: SoloContractState): List<SoloContractState> = simulate(contract, emptyList()).second

tailrec fun simulate(
    state: SoloContractState,
    series: List<SoloContractState>,
): Pair<SoloContractState, List<SoloContractState>> = when {
    state.farmer.finalState != null -> state to series.plus(state)
    else -> {
        val nextState = state.copy(
            farmer = state.farmer.copy(
                runningState = advanceOneMinute(
                    state.farmer.runningState,
                    state.farmer.constants,
                    state.timeElapsed
                ),
                finalState = when {
                    state.willFinish
                            || state.runningEggsLaid > state.finalGoal
                            || state.timeElapsed >= ONE_YEAR -> state.farmer.runningState
                    else -> state.farmer.finalState
                }
            ),
            goals = state.goals.map { goal ->
                when {
                    goal.moment == null && state.runningEggsLaid >= goal.amount -> {
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

tailrec fun simulate(
    state: CoopContractState,
): CoopContractState = when {
    state.endReached -> state
    else -> simulate(
        state.copy(
            farmers = state.farmers.map { farmer ->
                farmer.copy(
                    runningState = advanceOneMinute(farmer.runningState, farmer.constants, state.timeElapsed),
                    finalState = when {
                        state.willFinish
                                || state.runningEggsLaid > state.finalGoal
                                || state.timeElapsed >= ONE_YEAR -> farmer.runningState
                        else -> farmer.finalState
                    }
                )
            },
            goals = state.goals.map { goal ->
                when {
                    goal.moment == null && state.runningEggsLaid >= goal.amount -> {
                        goal.copy(moment = state.timeElapsed)
                    }
                    else -> goal
                }
            }.toSet(),
            timeElapsed = state.timeElapsed + ONE_MINUTE
        )
    )
}
