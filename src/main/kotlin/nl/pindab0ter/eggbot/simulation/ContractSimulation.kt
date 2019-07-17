package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.MathContext.DECIMAL64
import java.math.RoundingMode
import java.util.*

class ContractSimulation constructor(
    backup: EggInc.Backup,
    private val localContract: EggInc.LocalContract
) : Simulation(backup) {

    override val farm: EggInc.Simulation =
        backup.farmsList.find { it.contractId == localContract.contract.identifier }!!

    //
    // Basic info
    //

    val contractId: String = localContract.contract.identifier
    val contractName: String = localContract.contract.name
    val egg: EggInc.Egg = localContract.contract.egg
    var isActive: Boolean = true
    val finished: Boolean = localContract.finished

    //
    // Contract details
    //

    val elapsedTime: Duration = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())

    val timeRemaining: Duration = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)

    val goals: SortedMap<Int, BigDecimal> = localContract.contract.goalsList
        .mapIndexed { index, goal -> index to goal.targetAmount.toBigDecimal() }
        .toMap()
        .toSortedMap()

    val finalGoal: BigDecimal = goals[goals.lastKey()]!!

    //
    // Bottlenecks
    //

    abstract inner class BottleNeck {
        abstract val timeToReached: Duration
        abstract val isReached: Boolean
        val rateWhenReached: BigDecimal
            get() = currentEggLayingRatePerSecond * (population + internalHatcheryRatePerSecond * timeToReached.standardSeconds)
    }

    val noBottleNeck = object : BottleNeck() {
        override val timeToReached: Duration
            get() = timeToFinalGoal()
        override val isReached: Boolean
            get() = timeRemaining <= Duration.ZERO
    }

    val shippingRateBottleNeck = object : BottleNeck() {
        override val timeToReached: Duration
            get() = timeToMaxShippingRate
        override val isReached: Boolean
            get() = timeToReached < timeRemaining
    }

    val habitatsBottleNeck = object : BottleNeck() {
        override val timeToReached: Duration
            get() = timeToFullHabs
        override val isReached: Boolean
            get() = timeToReached < timeRemaining
    }

    val timeToFirstBottleNeck: Duration by lazy {
        minOf(
            shippingRateBottleNeck.timeToReached,
            habitatsBottleNeck.timeToReached,
            noBottleNeck.timeToReached
        )
    }

    val firstBottleNeckReached: BottleNeck by lazy {
        when {
            habitatsBottleNeck.timeToReached > timeRemaining && shippingRateBottleNeck.timeToReached > timeRemaining -> noBottleNeck
            habitatsBottleNeck.timeToReached <= shippingRateBottleNeck.timeToReached -> habitatsBottleNeck
            habitatsBottleNeck.timeToReached >= shippingRateBottleNeck.timeToReached -> shippingRateBottleNeck
            else -> noBottleNeck
        }
    }

    val eggLayingRateAtFirstBottleNeck: BigDecimal by lazy {
        (eggLayingRatePerSecond * (population + internalHatcheryRatePerSecond * firstBottleNeckReached.timeToReached.standardSeconds)) / population
    }


    val eggsToFirstBottleNeck: BigDecimal by lazy {
        (eggLayingRateAtFirstBottleNeck + currentEggLayingRatePerSecond)
            .divide(BigDecimal(2), RoundingMode.HALF_UP) * timeToFirstBottleNeck.standardSeconds + eggsLaid
    }

    val eggsRemainingAfterFirstBottleNeck: BigDecimal by lazy {
        finalGoal - eggsToFirstBottleNeck
    }

    val timeRequiredAfterFirstBottleNeck: Duration by lazy {
        eggsRemainingAfterFirstBottleNeck
            .divide(eggLayingRateAtFirstBottleNeck, DECIMAL64).toLong().toDuration()
    }


    //
    //  Projection
    //

    fun timeTo(goal: BigDecimal): Duration =
        (goal - eggsLaid).coerceAtLeast(ZERO).divide(eggLayingRatePerSecond, DECIMAL64).toLong().toDuration()

    fun timeToFinalGoal(): Duration = timeTo(finalGoal)

    // TODO: Take habitat and transport bottlenecks into account
    fun projectedTimeTo(goal: BigDecimal): Duration? = if (population == ZERO) null else {
        val accelerationFactor: BigDecimal =
            (eggLayingRatePerSecond * (population + populationIncreaseRatePerSecond) / population - eggLayingRatePerSecond) / ONE

        val duration: BigDecimal = (eggLayingRatePerSecond.negate() + sqrt(
            eggLayingRatePerSecond.pow(2) + BigDecimal(2) * accelerationFactor * (goal - eggsLaid).coerceAtLeast(ONE)
        )) / accelerationFactor

        duration.toLong().toDuration()
    }

    fun projectedTimeToFinalGoal(): Duration? = projectedTimeTo(finalGoal)

    fun projectedToFinish(): Boolean = projectedTimeToFinalGoal()?.let { it < timeRemaining } == true

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
