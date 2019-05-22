package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.times
import nl.pindab0ter.eggbot.toDateTime
import nl.pindab0ter.eggbot.toDuration
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Duration.ZERO
import java.math.BigDecimal
import java.math.MathContext.DECIMAL64
import java.math.RoundingMode
import java.util.*

class ContractSimulation private constructor(
    backup: EggInc.Backup,
    private val localContract: EggInc.LocalContract
) : HomeSimulation(backup) {

    override val farm: EggInc.Simulation =
        backup.farmsList.find { it.contractId == localContract.contract.identifier }!!

    //
    // Basic info
    //

    val contractId: String = localContract.contract.identifier
    val contractName: String = localContract.contract.name
    val egg: EggInc.Egg = localContract.contract.egg

    //
    // Contract details
    //

    val elapsedTime: Duration
        get() = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())

    val timeRemaining: Duration
        get() = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)

    val goals: SortedMap<Int, BigDecimal>
        get() = localContract.contract.goalsList
            .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
            .toMap()
            .toSortedMap()

    val finalGoal: BigDecimal
        get() = goals[goals.lastKey()]!!

    //
    // Bottlenecks
    //

    abstract inner class Projection {
        abstract val timeToReached: Duration
        abstract val isReached: Boolean
        val rateWhenReached: BigDecimal
            get() = currentEggLayingRatePerSecond * (population + internalHatcheryRatePerSecond * timeToReached.standardSeconds)
    }

    val noBottleNeck = object : Projection() {
        override val timeToReached: Duration
            get() = timeRequired
        override val isReached: Boolean
            get() = timeRemaining <= ZERO
    }

    val shippingRateBottleNeck = object : Projection() {
        override val timeToReached: Duration
            get() = timeToMaxShippingRate
        override val isReached: Boolean
            get() = timeToReached < timeRemaining
    }

    val habitatsBottleNeck = object : Projection() {
        override val timeToReached: Duration
            get() = timeToFullHabs
        override val isReached: Boolean
            get() = timeToReached < timeRemaining
    }

    //
    //  Projection
    //


    val timeToFirstProjection: Duration
        get() = minOf(
            shippingRateBottleNeck.timeToReached,
            habitatsBottleNeck.timeToReached,
            noBottleNeck.timeToReached
        )

    val firstProjectionReached: Projection
        get() = when {
            habitatsBottleNeck.timeToReached > timeRemaining && shippingRateBottleNeck.timeToReached > timeRemaining -> noBottleNeck
            habitatsBottleNeck.timeToReached <= shippingRateBottleNeck.timeToReached -> habitatsBottleNeck
            habitatsBottleNeck.timeToReached >= shippingRateBottleNeck.timeToReached -> shippingRateBottleNeck
            else -> noBottleNeck
        }

    val eggLayingRateAtFirstProjection: BigDecimal
        get() = (eggLayingRatePerSecond * (population + internalHatcheryRatePerSecond * firstProjectionReached.timeToReached.standardSeconds))
            .divide(population, DECIMAL64)

    val eggsToFirstProjection: BigDecimal
        get() = (eggLayingRateAtFirstProjection + currentEggLayingRatePerSecond)
            .divide(BigDecimal(2), RoundingMode.HALF_UP) * timeToFirstProjection.standardSeconds + eggsLaid

    val eggsRemainingAfterFirstProjection: BigDecimal
        get() = finalGoal - eggsToFirstProjection

    val timeRequiredAfterFirstProjection: Duration
        get() = eggsRemainingAfterFirstProjection
            .divide(eggLayingRateAtFirstProjection, DECIMAL64).toLong().toDuration()

    val timeRequired: Duration
        get() = (finalGoal - eggsLaid)
            .divide(eggLayingRatePerSecond, DECIMAL64).toLong().toDuration()

    // TODO: Add Internal Hatchery Calm
    val projectedTimeRequired: Duration
        get() = timeToFirstProjection + timeRequiredAfterFirstProjection


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