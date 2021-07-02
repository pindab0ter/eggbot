package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Egg
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.*
import org.joda.time.Duration
import java.math.BigDecimal

data class SoloContractState(
    val contractId: String,
    val contractName: String,
    val egg: Egg,
    val goals: Set<Goal>,
    val timeRemaining: Duration,
    val timeElapsed: Duration = Duration.ZERO,
    val farmer: Farmer,
) : Comparable<SoloContractState> {
    val reportedEggsLaid: BigDecimal
        get() = farmer.reportedState.eggsLaid
    val reportedEggsPerMinute: BigDecimal
        get() = eggIncrease(farmer.reportedState.habs, farmer.constants)
    val reportedPopulation: BigDecimal
        get() = farmer.reportedState.population
    val reportedPopulationIncreasePerMinute: BigDecimal
        get() = chickenIncrease(farmer.reportedState.habs, farmer.constants)
    val currentEggsLaid: BigDecimal
        get() = farmer.currentState?.eggsLaid ?: BigDecimal.ZERO
    val currentEggsPerMinute: BigDecimal
        get() = farmer.currentState?.let { state -> eggIncrease(state.habs, farmer.constants) } ?: BigDecimal.ZERO
    val runningEggsLaid: BigDecimal
        get() = farmer.runningState.eggsLaid
    val timeUpEggsLaid: BigDecimal
        get() = farmer.timeUpState?.eggsLaid ?: BigDecimal.ZERO
    val timeUpPercentageOfFinalGoal: BigDecimal
        get() = (timeUpEggsLaid / goals.last().amount) * 100
    val timeTillFinalGoal: Duration?
        get() = goals.last().moment
    val willFinish: Boolean
        get() = when {
            timeElapsed < timeRemaining -> runningEggsLaid >= goals.last().amount
            else -> timeUpEggsLaid >= goals.last().amount
        }
    val finishedIfBanked: Boolean
        get() = reportedEggsLaid < goals.last().amount && currentEggsLaid >= goals.last().amount
    val finished: Boolean
        get() = reportedEggsLaid >= goals.last().amount
    val goalsReached: Int
        get() = goals.count { (_, moment) -> moment != null }

    override fun compareTo(other: SoloContractState): Int = currentEggsLaid.compareTo(other.currentEggsLaid)

    companion object {
        operator fun invoke(
            backup: Backup,
            localContract: LocalContract,
        ): SoloContractState? {
            val farmer = Farmer(backup, localContract.contract!!.id)

            return if (farmer == null) null else SoloContractState(
                contractId = localContract.contract.id,
                contractName = localContract.contract.name,
                egg = localContract.contract.egg,
                goals = Goal.fromContract(localContract.contract, farmer.reportedState.eggsLaid),
                timeRemaining = localContract.timeRemaining,
                farmer = farmer
            )
        }

        val timeUpEggsLaidComparator = Comparator<SoloContractState> { one, other ->
            other.timeUpEggsLaid.compareTo(one.timeUpEggsLaid)
        }
    }
}