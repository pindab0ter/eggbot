package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.asyncMap
import nl.pindab0ter.eggbot.helpers.eggsLaid
import nl.pindab0ter.eggbot.helpers.finalGoal
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InActive.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress.*
import org.joda.time.Duration

sealed class CoopContractStatus(internal val priority: Int) : Comparable<CoopContractStatus> {
    data class NotFound(val coopId: String) : CoopContractStatus(0)

    sealed class InActive(priority: Int) : CoopContractStatus(priority) {
        abstract val coopStatus: CoopStatusResponse

        data class Finished(override val coopStatus: CoopStatusResponse) : CoopContractStatus.InActive(4)
        data class Abandoned(override val coopStatus: CoopStatusResponse) : CoopContractStatus.InActive(-1)
        data class Failed(override val coopStatus: CoopStatusResponse) : CoopContractStatus.InActive(-2)
    }

    sealed class InProgress(priority: Int) : CoopContractStatus(priority) {
        abstract val state: CoopContractState

        data class NotOnTrack(override val state: CoopContractState) : InProgress(1)
        data class OnTrack(override val state: CoopContractState) : InProgress(2)
        data class FinishedIfCheckedIn(override val state: CoopContractState) : InProgress(3)
    }

    override fun compareTo(other: CoopContractStatus): Int = this.priority.compareTo(other.priority)

    companion object {
        operator fun invoke(
            contract: Contract,
            coopStatus: CoopStatusResponse?,
            coopId: String,
            catchUp: Boolean,
            progressCallback: () -> Unit = { },
        ): CoopContractStatus = when {
            coopStatus == null ->
                NotFound(coopId)
            coopStatus.contributors.isEmpty() ->
                Abandoned(coopStatus)
            coopStatus.eggsLaid >= contract.finalGoal ->
                Finished(coopStatus)
            coopStatus.gracePeriodSecondsRemaining <= 0.0 && coopStatus.eggsLaid < contract.finalGoal ->
                Failed(coopStatus)
            else -> runBlocking(Dispatchers.Default) {
                val farmers = coopStatus.contributors.asyncMap(this.coroutineContext) { contributionInfo ->
                    AuxBrain.getFarmerBackup(contributionInfo.userId)
                        ?.let { Farmer(it, contract.id, catchUp) }
                        .also { progressCallback() }
                }.filterNotNull()

                val coopContractState = simulate(CoopContractState(contract, coopStatus, farmers))

                when {
                    coopContractState.finished -> Finished(coopStatus)
                    coopContractState.finishedIfCheckedIn -> FinishedIfCheckedIn(coopContractState)
                    coopContractState.willFinish -> OnTrack(coopContractState)
                    else -> NotOnTrack(coopContractState)
                }
            }
        }

        val currentEggsComparator = Comparator<CoopContractStatus> { one, other ->
            if (one is InProgress && other is InProgress)
                other.state.currentEggsLaid.compareTo(one.state.currentEggsLaid)
            else other.compareTo(one)
        }
        val timeTillFinalGoalComparator = Comparator<CoopContractStatus> { one, other ->
            if (one is InProgress && other is InProgress)
                (other.state.timeTillFinalGoal ?: Duration.ZERO).compareTo(one.state.timeTillFinalGoal ?: Duration.ZERO)
            else other.compareTo(one)
        }
    }
}
