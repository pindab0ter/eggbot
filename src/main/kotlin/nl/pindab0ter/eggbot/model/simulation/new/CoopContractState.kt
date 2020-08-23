package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Egg
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.asDaysHoursAndMinutes
import nl.pindab0ter.eggbot.helpers.sumByBigDecimal
import nl.pindab0ter.eggbot.helpers.timeRemaining
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
    val eggspected: BigDecimal = BigDecimal.ZERO,
    val farmers: List<Farmer>,
) {
    val eggsLaid: BigDecimal get() = farmers.sumByBigDecimal { farmer -> farmer.finalState.eggsLaid }
    val goalsReached: Int get() = goals.count { (_, moment) -> moment != null }
    val tokensAvailable: Int get() = farmers.sumBy { farmer -> farmer.constants.tokensAvailable }
    val tokensSpent: Int get() = farmers.sumBy { farmer -> farmer.constants.tokensSpent }

    constructor(localContract: LocalContract, public: Boolean, farmers: List<Farmer>) : this(
        contractId = localContract.contract!!.id,
        contractName = localContract.contract.name,
        coopId = localContract.coopId,
        egg = localContract.contract.egg,
        maxCoopSize = localContract.contract.maxCoopSize,
        public = public,
        goals = Goal.fromContract(localContract, farmers.sumByBigDecimal { farmer -> farmer.initialState.eggsLaid }),
        timeRemaining = localContract.timeRemaining,
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