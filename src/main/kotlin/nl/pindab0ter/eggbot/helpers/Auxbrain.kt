package nl.pindab0ter.eggbot.helpers

import com.auxbrain.ei.*
import nl.pindab0ter.eggbot.helpers.auxbrain.CommonResearch
import nl.pindab0ter.eggbot.helpers.auxbrain.EpicResearch
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal

val Backup.Game.soulBonus: Int get() = epicResearch.find { it.id == "soul_eggs" }!!.level
val Backup.Game.prophecyBonus: Int get() = epicResearch.find { it.id == "prophecy_bonus" }!!.level
fun Backup.farmFor(contractId: String): Backup.Simulation? = farms.firstOrNull { farm -> farm.contractId == contractId }
val Contract.finalGoal: BigDecimal get() = BigDecimal(goals.maxByOrNull { it.targetAmount }!!.targetAmount)
val LocalContract.finalGoal: BigDecimal get() = contract!!.finalGoal
val LocalContract.finished: Boolean get() = BigDecimal(lastAmountWhenRewardGiven) > contract?.finalGoal
val CoopStatusResponse.eggsLaid: BigDecimal get() = BigDecimal(totalAmount)
fun List<Backup>.findContract(contractId: String): LocalContract? = filter { backup ->
    backup.contracts?.contracts?.plus(backup.contracts.archive)?.any { contract ->
        contract.contract?.id == contractId
    } == true
}.maxByOrNull { backup -> backup.approxTime }?.let { backup ->
    backup.contracts?.contracts?.plus(backup.contracts.archive)?.find { contract ->
        contract.contract?.id == contractId
    }
}

val Backup.timeSinceBackup: Duration get() = Duration(approxTime.toDateTime(), DateTime.now())

val Backup.internalHatcheryMultiplier: BigDecimal
    get() = BigDecimal.ONE + BigDecimal(".05") * game!!.epicResearch[EpicResearch.EPIC_INT_HATCHERIES.ordinal].level

val Backup.internalHatcherySharing: BigDecimal
    get() = BigDecimal.ONE + BigDecimal(".10") * game!!.epicResearch[EpicResearch.INTERNAL_HATCH_SHARING.ordinal].level

val Backup.Simulation.internalHatcheryFlatIncreases: List<BigDecimal>
    get() = listOf(
        BigDecimal(2 * commonResearch[CommonResearch.INTERNAL_HATCHERY1.ordinal].level),
        BigDecimal(5 * commonResearch[CommonResearch.INTERNAL_HATCHERY2.ordinal].level),
        BigDecimal(10 * commonResearch[CommonResearch.INTERNAL_HATCHERY3.ordinal].level),
        BigDecimal(25 * commonResearch[CommonResearch.INTERNAL_HATCHERY4.ordinal].level),
        BigDecimal(5 * commonResearch[CommonResearch.MACHINE_LEARNING_INCUBATORS.ordinal].level),
        BigDecimal(50 * commonResearch[CommonResearch.NEURAL_LINKING.ordinal].level)
    )

val Backup.Simulation.habCapacityMultipliers: List<BigDecimal>
    get() = listOf(
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.HEN_HOUSE_REMODEL.ordinal].level,
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.MICROLUX_CHICKEN_SUITES.ordinal].level,
        BigDecimal.ONE + BigDecimal(".02") * commonResearch[CommonResearch.GRAV_PLATING.ordinal].level,
        BigDecimal.ONE + BigDecimal(".02") * commonResearch[CommonResearch.WORMHOLE_DAMPENING.ordinal].level
    )

val Backup.Simulation.shippingRateCommonResearchMultipliers: List<BigDecimal>
    get() = listOf(
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.IMPROVED_LEAFSPRINGS.ordinal].level,
        BigDecimal.ONE + BigDecimal(".10") * commonResearch[CommonResearch.LIGHTWEIGHT_BOXES.ordinal].level,
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.DRIVER_TRAINING.ordinal].level,
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.SUPER_ALLOY_FRAMES.ordinal].level,
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.QUANTUM_STORAGE.ordinal].level,
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.HOVER_UPGRADES.ordinal].level, // Assumes at least Hover Semi
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.DARK_CONTAINMENT.ordinal].level,
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.NEURAL_NET_REFINEMENT.ordinal].level,
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.HYPER_PORTALLING.ordinal].level,
    )

val Backup.shippingRateEpicResearchMultiplier: BigDecimal
    get() = BigDecimal.ONE + BigDecimal(".05") * game!!.epicResearch[EpicResearch.TRANSPORTATION_LOBBYISTS.ordinal].level

val Backup.Simulation.eggLayingCommonResearchMultipliers: List<BigDecimal>
    get() = listOf(
        BigDecimal.ONE + BigDecimal(".10") * commonResearch[CommonResearch.COMFORTABLE_NESTS.ordinal].level,
        BigDecimal.ONE + BigDecimal(".05") * commonResearch[CommonResearch.HEN_HOUSE_AC.ordinal].level,
        BigDecimal.ONE + BigDecimal(".15") * commonResearch[CommonResearch.IMPROVED_GENETICS.ordinal].level,
        BigDecimal.ONE + BigDecimal(".10") * commonResearch[CommonResearch.TIME_COMPRESSION.ordinal].level,
        BigDecimal.ONE + BigDecimal(".02") * commonResearch[CommonResearch.TIMELINE_DIVERSION.ordinal].level,
        BigDecimal.ONE + BigDecimal(".10") * commonResearch[CommonResearch.RELATIVITY_OPTIMIZATION.ordinal].level,
    )

val Backup.eggLayingEpicResearchMultiplier: BigDecimal
    get() = BigDecimal.ONE + BigDecimal(".05") * game!!.epicResearch[EpicResearch.EPIC_COMFY_NESTS.ordinal].level

val Backup.Simulation.baseShippingRate: BigDecimal
    get() = vehicles.foldIndexed(BigDecimal.ZERO) { index, acc, vehicleType ->
        when (vehicleType) {
            VehicleType.HYPERLOOP_TRAIN -> acc + vehicleType.capacity * hyperloopCars[index]
            else -> acc + vehicleType.capacity
        }
    }

val Backup.extraAwayTimePerSilo: Duration
    get() = Duration.ZERO.withDurationAdded(Duration.standardMinutes(6L),
        game!!.epicResearch[EpicResearch.SILO_CAPACITY.ordinal].level)

// @formatter:off
val HabLevel.capacity: BigDecimal get() = when(this) {
    HabLevel.NO_HAB ->                   BigDecimal.ZERO
    HabLevel.COOP ->                     BigDecimal(250)
    HabLevel.SHACK ->                    BigDecimal(500)
    HabLevel.SUPER_SHACK ->            BigDecimal(1_000)
    HabLevel.SHORT_HOUSE ->            BigDecimal(2_000)
    HabLevel.THE_STANDARD ->           BigDecimal(5_000)
    HabLevel.LONG_HOUSE ->            BigDecimal(10_000)
    HabLevel.DOUBLE_DECKER ->         BigDecimal(20_000)
    HabLevel.WAREHOUSE ->             BigDecimal(50_000)
    HabLevel.CENTER ->               BigDecimal(100_000)
    HabLevel.BUNKER ->               BigDecimal(200_000)
    HabLevel.EGGKEA ->               BigDecimal(500_000)
    HabLevel.HAB_1000 ->           BigDecimal(1_000_000)
    HabLevel.HANGAR ->             BigDecimal(2_000_000)
    HabLevel.TOWER ->              BigDecimal(5_000_000)
    HabLevel.HAB_10_000 ->        BigDecimal(10_000_000)
    HabLevel.EGGTOPIA ->          BigDecimal(25_000_000)
    HabLevel.MONOLITH ->          BigDecimal(50_000_000)
    HabLevel.PLANET_PORTAL ->    BigDecimal(100_000_000)
    HabLevel.CHICKEN_UNIVERSE -> BigDecimal(600_000_000)
    else ->                              BigDecimal.ZERO
}

val VehicleType.capacity: BigDecimal get() = when (this) {
    VehicleType.TRIKE ->                    BigDecimal(5_000)
    VehicleType.TRANSIT ->                 BigDecimal(15_000)
    VehicleType.PICKUP ->                  BigDecimal(50_000)
    VehicleType.VEHICLE_10_FOOT ->        BigDecimal(100_000)
    VehicleType.VEHICLE_24_FOOT ->        BigDecimal(250_000)
    VehicleType.SEMI ->                   BigDecimal(500_000)
    VehicleType.DOUBLE_SEMI ->          BigDecimal(1_000_000)
    VehicleType.FUTURE_SEMI ->          BigDecimal(5_000_000)
    VehicleType.MEGA_SEMI ->           BigDecimal(15_000_000)
    VehicleType.HOVER_SEMI ->          BigDecimal(30_000_000)
    VehicleType.QUANTUM_TRANSPORTER -> BigDecimal(50_000_000)
    VehicleType.HYPERLOOP_TRAIN ->     BigDecimal(50_000_000)
    else ->                                   BigDecimal.ZERO
}
// @formatter:on