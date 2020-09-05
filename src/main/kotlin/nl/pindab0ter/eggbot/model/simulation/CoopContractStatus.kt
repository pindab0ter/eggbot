package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatusResponse
import nl.pindab0ter.eggbot.helpers.eggsLaid
import nl.pindab0ter.eggbot.helpers.finalGoal
import nl.pindab0ter.eggbot.helpers.parallelMap
import nl.pindab0ter.eggbot.model.AuxBrain

sealed class CoopContractStatus {
    data class NotFound(
        val coopId: String,
    ) : CoopContractStatus()

    data class Abandoned(
        val coopStatus: CoopStatusResponse,
    ) : CoopContractStatus()

    data class Failed(
        val coopStatus: CoopStatusResponse,
    ) : CoopContractStatus()

    data class NotOnTrack(
        val state: CoopContractState,
    ) : CoopContractStatus()

    data class OnTrack(
        val state: CoopContractState,
    ) : CoopContractStatus()

    data class Finished(
        val coopStatus: CoopStatusResponse,
    ) : CoopContractStatus()

    data class FinishedIfCheckedIn(
        val state: CoopContractState,
    ) : CoopContractStatus()

    companion object {
        operator fun invoke(contract: Contract, coopId: String, catchUp: Boolean): CoopContractStatus {
            val coopStatus = AuxBrain.getCoopStatus(contract.id, coopId)

            return when {
                coopStatus == null ->
                    NotFound(coopId)
                coopStatus.gracePeriodSecondsRemaining <= 0.0 && coopStatus.eggsLaid < contract.finalGoal ->
                    Failed(coopStatus)
                coopStatus.contributors.isEmpty() ->
                    Abandoned(coopStatus)
                coopStatus.eggsLaid >= contract.finalGoal ->
                    Finished(coopStatus)
                else -> {
                    val farmers = coopStatus.contributors.parallelMap { contributionInfo ->
                        AuxBrain.getFarmerBackup(contributionInfo.userId)
                            ?.let { Farmer(it, contract.id, catchUp) }
                    }.filterNotNull()
                    val initialState = CoopContractState(contract, coopStatus, farmers)

                    if (initialState.finished) FinishedIfCheckedIn(initialState)

                    val simulatedState = simulate(initialState)

                    if (simulatedState.finished) OnTrack(simulatedState)
                    else NotOnTrack(simulatedState)
                }
            }
        }
    }
}

