package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.eggsLaid
import nl.pindab0ter.eggbot.helpers.finalGoal
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InActive.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress.*
import org.joda.time.Duration
import nl.pindab0ter.eggbot.model.simulation.Farmer.Companion as SimulationFarmer

sealed class CoopContractStatus(private val priority: Int) : Comparable<CoopContractStatus> {
    data class NotFound(val coopId: String) : CoopContractStatus(0)

    sealed class InActive(priority: Int) : CoopContractStatus(priority) {
        abstract val coopStatus: CoopStatus

        data class Finished(override val coopStatus: CoopStatus) : InActive(4)
        data class Abandoned(override val coopStatus: CoopStatus) : InActive(-1)
        data class Failed(override val coopStatus: CoopStatus) : InActive(-2)
    }

    sealed class InProgress(priority: Int) : CoopContractStatus(priority) {
        abstract val state: CoopContractState

        data class NotOnTrack(override val state: CoopContractState) : InProgress(1)
        data class OnTrack(override val state: CoopContractState) : InProgress(2)
        data class FinishedIfBanked(override val state: CoopContractState) : InProgress(3)
    }

    override fun compareTo(other: CoopContractStatus): Int = this.priority.compareTo(other.priority)

    companion object {
        operator fun invoke(
            contract: Contract,
            coopStatus: CoopStatus?,
            coopId: String,
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
                val simulationFarmers = coopStatus.contributors.mapNotNull { contributor -> SimulationFarmer(contributor) }
                // TODO: Communicate that we couldn't get the required information for a player where FarmInfo is null

                val coopContractState = simulate(CoopContractState(contract, coopStatus, simulationFarmers))

                when {
                    coopContractState.finished -> Finished(coopStatus)
                    coopContractState.finishedIfBanked -> FinishedIfBanked(coopContractState)
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
                (one.state.timeTillFinalGoal ?: Duration.ZERO).compareTo(other.state.timeTillFinalGoal ?: Duration.ZERO)
            else one.compareTo(other)
        }
    }
}
