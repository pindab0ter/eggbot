package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Backup
import com.auxbrain.ei.CoopStatus
import nl.pindab0ter.eggbot.model.auxbrain.*
import org.joda.time.Duration
import java.math.BigDecimal

data class Constants(
    val hatcherySharing: BigDecimal,
    val hatcheryRate: BigDecimal,
    val habCapacityMultiplier: BigDecimal,
    val eggLayingBonus: BigDecimal,
    val transportRate: BigDecimal,
    val tokensAvailable: Int,
    val tokensSpent: Int,
    val maxAwayTime: Duration,
) {
    constructor(
        backup: Backup,
        farm: Backup.Farm,
        coopArtifacts: List<Artifact> = emptyList(),
    ) : this(
        hatcherySharing = backup.hatcherySharingMultiplier,
        hatcheryRate = backup.hatcheryRateFor(farm),
        habCapacityMultiplier = backup.habCapacityMultiplierFor(farm),
        eggLayingBonus = backup.eggLayingRateMultiplierFor(farm, coopArtifacts),
        transportRate = backup.shippingRateFor(farm),
        tokensAvailable = farm.boostTokensReceived - farm.boostTokensGiven - farm.boostTokensSpent,
        tokensSpent = farm.boostTokensSpent,
        maxAwayTime = Silos.maxAwayTimeFor(backup, farm),
    )

    companion object {
        operator fun invoke(
            contributionInfo: CoopStatus.ContributionInfo
        ): Constants? {
            if (contributionInfo.farmInfo === null) {
                return null
            }

            return Constants(
                hatcherySharing = contributionInfo.farmInfo.hatcherySharingMultiplier,
                hatcheryRate = contributionInfo.farmInfo.hatcheryRate,
                habCapacityMultiplier = contributionInfo.farmInfo.habCapacityMultiplier,
                eggLayingBonus = contributionInfo.farmInfo.eggLayingRateMultiplier,
                transportRate = contributionInfo.farmInfo.shippingRate,
                tokensAvailable = contributionInfo.boostTokens ?: 0,
                tokensSpent = contributionInfo.boostTokensSpent ?: 0,
                maxAwayTime = contributionInfo.farmInfo.maxAwayTime,
            )
        }
    }
}