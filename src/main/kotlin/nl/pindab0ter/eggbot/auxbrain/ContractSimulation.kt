package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.*
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

    override val farm: EggInc.Simulation = backup.farmsList.find { it.contractId == localContract.contract.identifier }!!

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
            get() = currentEggLayingRatePerMinute * (population + internalHatcheryRatePerMinute * timeToReached.standardMinutes)

    }

    val noBottleNeck = object : Projection() {
        override val timeToReached: Duration
            get() = timeRequired
        override val isReached: Boolean
            get() = timeRemaining <= ZERO
    }

    val shippingRateBottleNeck = object : Projection() {
        override val timeToReached: Duration
            get() = ((shippingRatePerMinute / eggLayingRatePerMinute * population - population) / internalHatcheryRatePerMinute).toLong().toDuration()
        override val isReached: Boolean
            get() = timeToReached < timeRemaining
    }

    val habitatsBottleNeck = object : Projection() {
        override val timeToReached: Duration
            get() = farm.habsList
                .mapIndexed { index, hab -> hab.maxCapacity - farm.habPopulation[index] }
                .map { roomToGrow ->
                    roomToGrow.divide(internalHatcheryRatePerMinute, RoundingMode.HALF_UP).toLong().toDuration()
                }
                .sum()
                .dividedBy(farm.habsList.foldIndexed(0L) { index, acc, hab ->
                    acc + if (hab.capacity.toInt() >= farm.habPopulationList[index]) 0L else 1L
                }.coerceAtLeast(1))
        override val isReached: Boolean
            get() = timeToReached < timeRemaining
    }

    val timeToFirstProjection: Duration
        get() = minOf(
            shippingRateBottleNeck.timeToReached,
            habitatsBottleNeck.timeToReached,
            noBottleNeck.timeToReached
        )

    val firstProjectionReached: Projection
        get() = when {
            habitatsBottleNeck.timeToReached <= shippingRateBottleNeck.timeToReached -> habitatsBottleNeck
            habitatsBottleNeck.timeToReached >= shippingRateBottleNeck.timeToReached -> shippingRateBottleNeck
            else -> noBottleNeck
        }

    val eggLayingRateAtFirstProjection: BigDecimal
        get() = (eggLayingRatePerMinute * (population + internalHatcheryRatePerMinute * firstProjectionReached.timeToReached.standardMinutes))
            .divide(population, DECIMAL64)

    val eggsToFirstProjection: BigDecimal
        get() = (eggLayingRateAtFirstProjection + currentEggLayingRatePerMinute)
            .divide(BigDecimal(2), RoundingMode.HALF_UP) * timeToFirstProjection.standardMinutes + eggsLaid

    val eggsRemainingAfterFirstProjection: BigDecimal
        get() = finalGoal - eggsToFirstProjection

    val timeRequiredAfterFirstProjection: Duration
        get() = eggsRemainingAfterFirstProjection
            .divide(eggLayingRateAtFirstProjection, DECIMAL64).toLong().toDuration()

    val projectedTimeRequired: Duration
        get() = timeToFirstProjection + timeRequiredAfterFirstProjection

    val timeRequired: Duration
        get() = (finalGoal - eggsLaid)
            .divide(eggLayingRatePerMinute, DECIMAL64).toLong().toDuration()

    /*val timeToMaxShippingRate: Duration
        get() = Duration(((shippingRatePerMinute / eggLayingRatePerSecond * population - population) / internalHatcheryRatePerMinute).toLong())

    val shippingRateIsBottleNeck = timeToMaxShippingRate < timeRemaining

    // Disregards Internal Hatchery Sharing and takes the average of each hab's time to full.
    // TODO: Include Internal Hatchery Sharing and soft knees for each full hab
    val timeToFullHabs: Duration
        get() = farm.habsList
            .mapIndexed { index, hab -> hab.maxCapacity - farm.habPopulation[index] }
            .map { roomToGrow ->
                roomToGrow
                    .divide(internalHatcheryRatePerMinute, RoundingMode.HALF_UP)
                    .toLong()
                    .toDuration()
            }
            .sum()
            .dividedBy(farm.habsList.foldIndexed(0L) { index, acc, hab ->
                acc + if (hab.capacity.toInt() >= farm.habPopulationList[index]) 0L else 1L
            }.coerceAtLeast(1))

    val habsIsBottleNeck = timeToFullHabs < timeRemaining*/

    //
    //  Projection
    //

    // val finalTarget
    //     get() = eggsLaid +
    //             (eggLayingRatePerSecond * timeRemaining) +
    //             (BigDecimal(0.5) * (eggLayingBaseRatePerSecond * eggLayingBonus)) *
    //             timeRemaining *
    //             timeRemaining *
    //             internalHatcheryCalm

    // val finalTargetWithCalm
    //     get() = eggsLaid +
    //             (eggLayingRatePerSecond * timeRemaining) +
    //             (BigDecimal(0.5) * (eggLayingBaseRatePerSecond * eggLayingBonus)) *
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