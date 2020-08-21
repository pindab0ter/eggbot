package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.*
import org.joda.time.Duration
import java.math.BigDecimal

data class Constants(
    val internalHatcherySharing: BigDecimal,
    val internalHatcheryRate: BigDecimal,
    val habCapacityMultiplier: BigDecimal,
    val eggLayingBonus: BigDecimal,
    val transportRate: BigDecimal,
    val maxAwayTime: Duration,
) {
    constructor(backup: Backup, farm: Backup.Simulation) : this(
        internalHatcherySharing = backup.internalHatcherySharing,
        internalHatcheryRate = farm.internalHatcheryFlatIncreases.sum()
            .multiply(backup.internalHatcheryMultiplier),
        habCapacityMultiplier = farm.habCapacityMultipliers.sum(),
        eggLayingBonus = farm.eggLayingCommonResearchMultipliers
            .plus(backup.eggLayingEpicResearchMultiplier)
            .product(),
        transportRate = farm.baseShippingRate.multiply(
            farm.shippingRateCommonResearchMultipliers
                .plus(backup.shippingRateEpicResearchMultiplier)
                .product()
        ),
        maxAwayTime = Duration.standardHours(1L)
            .plus(backup.extraAwayTimePerSilo)
            .multipliedBy(farm.silosOwned.toLong())
    )
}