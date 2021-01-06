package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.activeSoloArtifactsFor
import nl.pindab0ter.eggbot.helpers.auxbrain.*
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
    val activeArtifacts: List<Artifact>,
) {
    constructor(
        backup: Backup,
        farm: Backup.Farm,
        activeCoopArtifacts: List<Artifact> = emptyList()
    ) : this(
        internalHatcherySharing = Hatchery.sharingMultiplierFor(backup),
        internalHatcheryRate = Hatchery.multiplierFor(backup, farm),
        habCapacityMultiplier = Habs.multiplierFor(farm, backup),
        eggLayingBonus = EggLayingRate.multiplierFor(farm, backup, activeCoopArtifacts),
        transportRate = Transport.multiplierFor(backup, farm),
        tokensAvailable = farm.boostTokensReceived - farm.boostTokensGiven - farm.boostTokensSpent,
        tokensSpent = farm.boostTokensSpent,
        maxAwayTime = Silos.maxAwayTimeFor(backup, farm),
        activeArtifacts = backup.activeSoloArtifactsFor(farm).plus(activeCoopArtifacts)
    )
}