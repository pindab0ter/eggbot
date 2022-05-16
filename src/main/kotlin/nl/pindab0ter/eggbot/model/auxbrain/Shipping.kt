package nl.pindab0ter.eggbot.model.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Name.INTERSTELLAR_COMPASS
import com.auxbrain.ei.Artifact.Name.QUANTUM_STONE
import com.auxbrain.ei.Backup
import com.auxbrain.ei.Backup.Farm
import com.auxbrain.ei.VehicleType
import nl.pindab0ter.eggbot.helpers.product
import nl.pindab0ter.eggbot.helpers.productOf
import nl.pindab0ter.eggbot.helpers.times
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO

fun Backup.shippingRateResearchMultiplierFor(farm: Farm): BigDecimal = farm.shippingRateCommonResearchMultipliers
    .plus(shippingRateEpicResearchMultiplier)
    .product()

fun Backup.shippingRateArtifactsMultiplierFor(farm: Farm): BigDecimal = artifactsFor(farm).shippingRateMultiplier

fun Backup.shippingRateFor(farm: Farm): BigDecimal = farm.baseShippingRate
    .multiply(shippingRateResearchMultiplierFor(farm))
    .multiply(shippingRateArtifactsMultiplierFor(farm))

val shippingRateArtifacts = listOf(
    INTERSTELLAR_COMPASS,
    QUANTUM_STONE,
)

private val List<Artifact>.shippingRateMultiplier
    get() = filter { artifact ->
        artifact.name in shippingRateArtifacts
    }.productOf { artifact ->
        artifact.multiplier
    }

private val Farm.shippingRateCommonResearchMultipliers: List<BigDecimal>
    get() = listOf(
        ONE + BigDecimal(".05") * commonResearch[CommonResearch.IMPROVED_LEAFSPRINGS.ordinal].level,
        ONE + BigDecimal(".10") * commonResearch[CommonResearch.LIGHTWEIGHT_BOXES.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[CommonResearch.DRIVER_TRAINING.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[CommonResearch.SUPER_ALLOY_FRAMES.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[CommonResearch.QUANTUM_STORAGE.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[CommonResearch.HOVER_UPGRADES.ordinal].level, // Assumes at least Hover Semi
        ONE + BigDecimal(".05") * commonResearch[CommonResearch.DARK_CONTAINMENT.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[CommonResearch.NEURAL_NET_REFINEMENT.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[CommonResearch.HYPER_PORTALLING.ordinal].level,
    )

private val Backup.shippingRateEpicResearchMultiplier: BigDecimal
    get() = ONE + BigDecimal(".05") * game!!.epicResearch[EpicResearch.TRANSPORTATION_LOBBYISTS.ordinal].level

val Farm.baseShippingRate: BigDecimal
    get() = vehicles.foldIndexed(ZERO) { index, acc, vehicleType ->
        when (vehicleType) {
            VehicleType.HYPERLOOP_TRAIN -> acc + vehicleType.capacity * hyperloopCars[index]
            else -> acc + vehicleType.capacity
        }
    }

// @formatter:off
private val VehicleType.capacity: BigDecimal get() = when (this) {
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
}
// @formatter:on