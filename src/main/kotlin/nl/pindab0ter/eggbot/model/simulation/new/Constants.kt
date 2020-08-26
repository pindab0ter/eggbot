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
    val tokensAvailable: Int,
    val tokensSpent: Int,
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
        tokensAvailable = farm.boostTokensReceived - farm.boostTokensGiven - farm.boostTokensSpent,
        // TODO: Is this always right?
        // It seems like bock counted the 18 tokens that I sent Zman as tokens I spent when I ran the coop command
        // https://discordapp.com/channels/485162044652388384/717043294659543133/747909593346211923
        tokensSpent = farm.boostTokensSpent,
        maxAwayTime = Duration.standardHours(1L)
            .plus(backup.extraAwayTimePerSilo)
            .multipliedBy(farm.silosOwned.toLong())
    )
}