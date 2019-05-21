package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.toDateTime
import nl.pindab0ter.eggbot.toDuration
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal

class ContractSimulation private constructor(
    backup: EggInc.Backup,
    private val localContract: EggInc.LocalContract
) : HomeSimulation(backup) {

    //
    // Basic info
    //

    val contractId: String = localContract.contract.identifier
    val contractName: String = localContract.contract.name
    val egg: EggInc.Egg = localContract.contract.egg

    //
    // Contract details
    //

    val elapsedTime
        get() = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())

    val timeRemaining
        get() = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)

    val goals: Map<Int, BigDecimal>
        get() = localContract.contract.goalsList
            .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
            .toMap()

    //
    //  Projection
    //

    // val finalTarget
    //     get() = eggsLaid +
    //             (eggLayingRate * timeRemaining) +
    //             (BigDecimal(0.5) * (eggLayingBaseRate * eggLayingBonus)) *
    //             timeRemaining *
    //             timeRemaining *
    //             internalHatcheryCalm

    // val finalTargetWithCalm
    //     get() = eggsLaid +
    //             (eggLayingRate * timeRemaining) +
    //             (BigDecimal(0.5) * (eggLayingBaseRate * eggLayingBonus)) *
    //             timeRemaining *
    //             timeRemaining

    companion object {
        operator fun invoke(
            backup: EggInc.Backup,
            contractId: String
        ): ContractSimulation? = backup.contracts.contractsList.find { localContract ->
            localContract.contract.identifier == contractId
        }?.let { contract ->
            ContractSimulation(backup, contract)
        }
    }
}