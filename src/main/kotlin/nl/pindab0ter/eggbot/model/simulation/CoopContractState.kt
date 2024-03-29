package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import com.auxbrain.ei.Egg
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FOUR
import org.joda.time.Duration
import java.math.BigDecimal

data class CoopContractState(
    val contractId: String,
    val contractName: String,
    val coopId: String,
    val egg: Egg,
    val maxCoopSize: Int,
    val public: Boolean,
    val goals: Set<Goal>,
    val timeRemaining: Duration,
    val timeElapsed: Duration = Duration.ZERO,
    val farmers: List<Farmer>,
) {
    val currentPopulation: BigDecimal
        get() = farmers.sumOf { farmer ->
            farmer.currentState?.population ?: BigDecimal.ZERO
        }
    val currentPopulationIncreasePerMinute: BigDecimal
        get() = farmers.sumOf { farmer ->
            chickenIncrease(
                farmer.currentState?.habs ?: emptyList(),
                farmer.constants
            ).multiply(FOUR - (farmer.currentState?.habs?.fullCount() ?: BigDecimal.ZERO))

        }
    val currentEggsLaid: BigDecimal
        get() = farmers.sumOf { farmer -> farmer.currentEggsLaid }
    val reportedEggsLaid: BigDecimal
        get() = farmers.sumOf { farmer -> farmer.reportedEggsLaid }
    val unreportedEggsLaid: BigDecimal
        get() = currentEggsLaid - reportedEggsLaid
    val currentEggsPerMinute: BigDecimal
        get() = farmers.sumOf { farmer ->
            eggIncrease(farmer.currentState?.habs ?: emptyList(), farmer.constants)
        }
    val runningEggsLaid: BigDecimal
        get() = farmers.sumOf { farmer -> farmer.runningState.eggsLaid }
    val timeUpEggsLaid: BigDecimal
        get() = farmers.sumOf { farmer -> farmer.timeUpState?.eggsLaid ?: BigDecimal.ZERO }
    val timeUpPercentageOfFinalGoal: BigDecimal
        get() = (timeUpEggsLaid / goals.last().amount) * 100
    val timeTillFinalGoal: Duration?
        get() = goals.last().moment
    val tokensAvailable: Int
        get() = farmers.sumOf { farmer -> farmer.constants.tokensAvailable }
    val tokensSpent: Int
        get() = farmers.sumOf { farmer -> farmer.constants.tokensSpent }
    val goalsReached: Int
        get() = goals.count { (_, moment) -> moment != null }
    val willFinish: Boolean
        get() = when {
            timeElapsed < timeRemaining -> runningEggsLaid >= goals.last().amount
            else -> timeUpEggsLaid >= goals.last().amount
        }
    val finishedIfBanked: Boolean
        get() = reportedEggsLaid < goals.last().amount && currentEggsLaid >= goals.last().amount
    val finished: Boolean
        get() = reportedEggsLaid >= goals.last().amount

    constructor(
        contract: Contract,
        coopStatus: CoopStatus,
        farmers: List<Farmer>,
    ) : this(
        contractId = contract.id,
        contractName = contract.name,
        coopId = coopStatus.coopId,
        egg = contract.egg,
        maxCoopSize = contract.maxCoopSize,
        public = coopStatus.public_,
        goals = Goal.fromContract(contract, farmers.sumOf { farmer: Farmer -> farmer.reportedState.eggsLaid }),
        timeRemaining = coopStatus.secondsRemaining.toDuration(),
        farmers = farmers
    )
}