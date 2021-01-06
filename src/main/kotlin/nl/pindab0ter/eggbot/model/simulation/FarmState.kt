package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.HabsStatus
import nl.pindab0ter.eggbot.helpers.HabsStatus.Free
import nl.pindab0ter.eggbot.helpers.eggIncrease
import nl.pindab0ter.eggbot.helpers.habsStatus
import nl.pindab0ter.eggbot.helpers.sumByBigDecimal
import org.joda.time.Duration
import java.math.BigDecimal

data class FarmState(
    val habs: List<Hab>,
    val eggsLaid: BigDecimal = BigDecimal.ZERO,
    val habsStatus: HabsStatus = Free,
    val transportBottleneck: Duration? = null,
) {
    constructor(farm: Backup.Farm, backup: Backup, constants: Constants) : this(
        habs = Hab.fromFarm(farm, backup),
        eggsLaid = farm.eggsLaid.toBigDecimal(),
        habsStatus = habsStatus(Hab.fromFarm(farm, backup), Duration.ZERO),
        transportBottleneck = when {
            eggIncrease(Hab.fromFarm(farm, backup), constants) >= constants.transportRate -> Duration.ZERO
            else -> null
        }
    )

    val population: BigDecimal get() = habs.sumByBigDecimal(Hab::population)
}