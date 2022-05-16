package nl.pindab0ter.eggbot.model.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Name.GUSSET
import com.auxbrain.ei.Backup
import com.auxbrain.ei.Backup.Farm
import com.auxbrain.ei.HabLevel
import com.auxbrain.ei.HabLevel.*
import nl.pindab0ter.eggbot.helpers.product
import nl.pindab0ter.eggbot.helpers.productOf
import nl.pindab0ter.eggbot.helpers.times
import nl.pindab0ter.eggbot.model.auxbrain.CommonResearch.*
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO

fun Farm.habCapacityResearchMultiplier(): BigDecimal = habCapacityResearchMultipliers.product()
fun Backup.habCapacityArtifactsMultiplierFor(farm: Farm): BigDecimal = artifactsFor(farm).habCapacityMultiplier
fun Backup.habCapacityMultiplierFor(farm: Farm): BigDecimal = farm.habCapacityResearchMultiplier()
    .multiply(habCapacityArtifactsMultiplierFor(farm))

val habCapacityArtifacts = listOf(
    GUSSET,
)

private val Farm.habCapacityResearchMultipliers: List<BigDecimal>
    get() = listOf(
        ONE + BigDecimal(".05") * commonResearch[HEN_HOUSE_REMODEL.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[MICROLUX_CHICKEN_SUITES.ordinal].level,
        ONE + BigDecimal(".02") * commonResearch[GRAV_PLATING.ordinal].level,
        ONE + BigDecimal(".02") * commonResearch[WORMHOLE_DAMPENING.ordinal].level
    )

val List<Artifact>.habCapacityMultiplier
    get() = filter { artifact ->
        artifact.name in habCapacityArtifacts
    }.productOf(Artifact::multiplier)

val Farm.baseHabCapacity: BigDecimal
    get() = habs.sumOf(HabLevel::capacity)

// @formatter:off
val HabLevel.capacity: BigDecimal get() = when(this) {
    NO_HAB ->                              ZERO
    COOP ->                     BigDecimal(250)
    SHACK ->                    BigDecimal(500)
    SUPER_SHACK ->            BigDecimal(1_000)
    SHORT_HOUSE ->            BigDecimal(2_000)
    THE_STANDARD ->           BigDecimal(5_000)
    LONG_HOUSE ->            BigDecimal(10_000)
    DOUBLE_DECKER ->         BigDecimal(20_000)
    WAREHOUSE ->             BigDecimal(50_000)
    CENTER ->               BigDecimal(100_000)
    BUNKER ->               BigDecimal(200_000)
    EGGKEA ->               BigDecimal(500_000)
    HAB_1000 ->           BigDecimal(1_000_000)
    HANGAR ->             BigDecimal(2_000_000)
    TOWER ->              BigDecimal(5_000_000)
    HAB_10_000 ->        BigDecimal(10_000_000)
    EGGTOPIA ->          BigDecimal(25_000_000)
    MONOLITH ->          BigDecimal(50_000_000)
    PLANET_PORTAL ->    BigDecimal(100_000_000)
    CHICKEN_UNIVERSE -> BigDecimal(600_000_000)
}
// @formatter:on
