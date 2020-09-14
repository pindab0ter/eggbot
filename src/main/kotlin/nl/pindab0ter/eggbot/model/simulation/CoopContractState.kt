package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatusResponse
import com.auxbrain.ei.Egg
import nl.pindab0ter.eggbot.helpers.asDaysHoursAndMinutes
import nl.pindab0ter.eggbot.helpers.sumByBigDecimal
import nl.pindab0ter.eggbot.helpers.toDuration
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
    val elapsed: Duration = Duration.ZERO,
    val farmers: List<Farmer>,
) {
    val initialEggsLaid: BigDecimal get() = farmers.sumByBigDecimal { farmer -> farmer.initialState.eggsLaid }
    val finalEggsLaid: BigDecimal get() = farmers.sumByBigDecimal { farmer -> farmer.finalState.eggsLaid }
    val tokensAvailable: Int get() = farmers.sumBy { farmer -> farmer.constants.tokensAvailable }
    val tokensSpent: Int get() = farmers.sumBy { farmer -> farmer.constants.tokensSpent }
    val goalsReached: Int get() = goals.count { (_, moment) -> moment != null }
    val willFinish: Boolean get() = goals.all { (_, moment) -> moment != null }
    val willFinishInTime: Boolean get() = goals.all { (_, moment) -> moment != null && moment <= timeRemaining }
    val finished: Boolean get() = goals.all { (_, moment) -> moment == Duration.ZERO }

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
        goals = Goal.fromContract(contract, farmers.sumByBigDecimal { farmer -> farmer.initialState.eggsLaid }),
        timeRemaining = coopStatus.secondsRemaining.toDuration(),
        farmers = farmers
    )

    override fun toString(): String = "${this::class.simpleName}(" +
            "contractId=${contractId}, " +
            "contractName=${contractName}, " +
            "coopId=${coopId}, " +
            "goals=${goals}, " +
            "timeRemaining=${timeRemaining.asDaysHoursAndMinutes()}, " +
            "elapsed=${elapsed.asDaysHoursAndMinutes()}" +
            ")"
}