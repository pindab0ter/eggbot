package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc.Backup
import com.auxbrain.ei.EggInc.VehicleType
import nl.pindab0ter.eggbot.auxbrain.CommonResearch.*
import nl.pindab0ter.eggbot.toDateTime
import nl.pindab0ter.eggbot.toDuration
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.MathContext.DECIMAL128

class Simulation(val backup: Backup, val contractId: String) {
    private val farm = backup.farmsList.find { it.contractId == contractId }!!
    private val localContract = backup.contracts.contractsList.find { it.contract.identifier == contractId }!!
    val egg = localContract.contract.egg
    val contractName = localContract.contract.name

    val eggLayingBonus = listOf(
        1 + .10 * farm.commonResearchList[COMFORTABLE_NESTS.ordinal].level,
        1 + .05 * farm.commonResearchList[HEN_HOUSE_AC.ordinal].level,
        1 + .15 * farm.commonResearchList[IMPROVED_GENETICS.ordinal].level,
        1 + .10 * farm.commonResearchList[TIME_COMPRESSION.ordinal].level,
        1 + .02 * farm.commonResearchList[TIMELINE_DIVERSION.ordinal].level,
        1 + .05 * backup.data.epicResearchList[EpicResearch.EPIC_COMFY_NESTS.ordinal].level
    ).reduce { acc, bonus -> acc * bonus }.toBigDecimal()

    val internalHatcheryRateMinute = listOf(
        2 * farm.commonResearchList[INTERNAL_HATCHERY1.ordinal].level,
        5 * farm.commonResearchList[INTERNAL_HATCHERY2.ordinal].level,
        10 * farm.commonResearchList[INTERNAL_HATCHERY3.ordinal].level,
        25 * farm.commonResearchList[INTERNAL_HATCHERY4.ordinal].level,
        5 * farm.commonResearchList[MACHINE_LEARNING_INCUBATORS.ordinal].level,
        50 * farm.commonResearchList[NEURAL_LINKING.ordinal].level
    ).sum()
        .times(1 + .05 * backup.data.epicResearchList[EpicResearch.EPIC_INT_HATCHERIES.ordinal].level)
        .times(4) // Assumes four habitats
        .toBigDecimal()

    val eggLayingRateSecond = farm.numChickens.toBigDecimal() * eggLayingBonus.divide(BigDecimal(30), DECIMAL128)

    val shippingRateBonus = listOf(
        1 + .05 * farm.commonResearchList[IMPROVED_LEAFSPRINGS.ordinal].level,
        1 + .10 * farm.commonResearchList[LIGHTWEIGHT_BOXES.ordinal].level,
        1 + .05 * farm.commonResearchList[DRIVER_TRAINING.ordinal].level,
        1 + .05 * farm.commonResearchList[SUPER_ALLOY_FRAMES.ordinal].level,
        1 + .05 * farm.commonResearchList[QUANTUM_STORAGE.ordinal].level,
        1 + .05 * farm.commonResearchList[HOVER_UPGRADES.ordinal].level,
        1 + .05 * farm.commonResearchList[DARK_CONTAINMENT.ordinal].level,
        1 + .05 * farm.commonResearchList[NEURAL_NET_REFINEMENT.ordinal].level,
        1 + .05 * backup.data.epicResearchList[EpicResearch.TRANSPORTATION_LOBBYISTS.ordinal].level
    ).reduce { acc, bonus -> acc * bonus }.toBigDecimal()

    val shippingRateSecond = farm.vehiclesList
        .sumBy(VehicleType::capacity)
        .let(Int::toBigDecimal)
        .times(shippingRateBonus)

    val effectiveEggLayingRateSecond = minOf(eggLayingRateSecond, shippingRateSecond)
    val effectiveEggLayingRateHour = effectiveEggLayingRateSecond * BigDecimal(60 * 60)

    val elapsedTime = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())
    val timeRemaining = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)
    private val secondsRemaining = timeRemaining.standardSeconds
    val requiredEggs = localContract.contract.goalsList[localContract.contract.goalsList.size - 1].targetAmount
    private val eggLayingBaseRate = internalHatcheryRateMinute.divide(BigDecimal(60 * 30), DECIMAL128) // 1/3rd per second?
    val eggsLaid = farm.eggsLaid

    val finalTarget = BigDecimal(eggsLaid) +
            (eggLayingRateSecond * secondsRemaining.toBigDecimal()) +
            (BigDecimal(0.5) * (eggLayingBaseRate * eggLayingBonus)) *
            secondsRemaining.toBigDecimal() *
            secondsRemaining.toBigDecimal() *
            BigDecimal(1 + 0.10 * backup.data.epicResearchList[EpicResearch.INTERNAL_HATCH_CALM.ordinal].level)

    val finalTargetWithCalm = BigDecimal(eggsLaid) +
            (eggLayingRateSecond * secondsRemaining.toBigDecimal()) +
            (BigDecimal(0.5) * (eggLayingBaseRate * eggLayingBonus)) *
            secondsRemaining.toBigDecimal() *
            secondsRemaining.toBigDecimal()
}
