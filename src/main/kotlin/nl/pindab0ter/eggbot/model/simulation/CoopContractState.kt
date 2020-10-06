package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatusResponse
import com.auxbrain.ei.Egg
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FOUR
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

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
    val currentEggsLaid: BigDecimal
        get() = farmers.sumByBigDecimal { farmer ->
            farmer.caughtUpState?.eggsLaid ?: farmer.reportedState.eggsLaid
        }
    val currentEggsPerMinute: BigDecimal
        get() = farmers.sumByBigDecimal { farmer ->
            eggIncrease(farmer.caughtUpState?.habs ?: farmer.reportedState.habs, farmer.constants)
        }
    val currentPopulation: BigDecimal
        get() = farmers.sumByBigDecimal { farmer ->
            farmer.caughtUpState?.population ?: farmer.reportedState.population
        }
    val currentPopulationIncreasePerMinute: BigDecimal
        get() = farmers.sumByBigDecimal { farmer ->
            (farmer.caughtUpState ?: farmer.reportedState).let { state ->
                chickenIncrease(state.habs, farmer.constants).multiply(FOUR - state.habs.fullCount())
            }
        }
    val reportedEggsLaid: BigDecimal
        get() = farmers.sumByBigDecimal { farmer -> farmer.reportedState.eggsLaid }
    val caughtUpEggsLaid: BigDecimal
        get() = farmers.sumByBigDecimal { farmer -> farmer.caughtUpState?.eggsLaid ?: ZERO }
    val runningEggsLaid: BigDecimal
        get() = farmers.sumByBigDecimal { farmer -> farmer.runningState.eggsLaid }
    val timeUpEggsLaid: BigDecimal
        get() = farmers.sumByBigDecimal { farmer -> farmer.timeUpState?.eggsLaid ?: ZERO }
    val timeUpPercentageOfFinalGoal: BigDecimal
        get() = (timeUpEggsLaid / goals.last().amount) * 100
    val timeTillFinalGoal: Duration?
        get() = goals.last().moment
    val tokensAvailable: Int
        get() = farmers.sumBy { farmer -> farmer.constants.tokensAvailable }
    val tokensSpent: Int
        get() = farmers.sumBy { farmer -> farmer.constants.tokensSpent }
    val goalsReached: Int
        get() = goals.count { (_, moment) -> moment != null }
    val willFinish: Boolean
        get() = timeUpEggsLaid >= goals.last().amount
    val finishedIfCheckedIn: Boolean
        get() = reportedEggsLaid < goals.last().amount && caughtUpEggsLaid >= goals.last().amount
    val finished: Boolean
        get() = reportedEggsLaid >= goals.last().amount

    constructor(
        contract: Contract,
        coopStatus: CoopStatusResponse,
        farmers: List<Farmer>,
    ) : this(
        contractId = contract.id,
        contractName = contract.name,
        coopId = coopStatus.coopId,
        egg = contract.egg,
        maxCoopSize = contract.maxCoopSize,
        public = coopStatus.public,
        goals = Goal.fromContract(contract, farmers.sumByBigDecimal { farmer -> farmer.reportedState.eggsLaid }),
        timeRemaining = coopStatus.secondsRemaining.toDuration(),
        farmers = farmers
    )
}