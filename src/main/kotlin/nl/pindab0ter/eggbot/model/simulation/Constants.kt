package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.utilities.*
import java.math.BigDecimal

data class Constants(
    val internalHatcherySharing: BigDecimal,
    val internalHatcheryRate: BigDecimal,
    val habCapacityMultiplier: BigDecimal,
    val eggLayingBonus: BigDecimal,
    val transportRate: BigDecimal,
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
        )
    )
}