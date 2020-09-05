package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.HabsStatus.Free
import org.joda.time.Duration
import java.math.BigDecimal

data class FarmState(
    val habs: List<Hab>,
    val eggsLaid: BigDecimal = BigDecimal.ZERO,
    val habsStatus: HabsStatus = Free,
    val transportBottleneck: Duration? = null,
) {
    constructor(farm: Backup.Simulation, constants: Constants) : this(
        habs = Hab.fromFarm(farm),
        eggsLaid = farm.eggsLaid.toBigDecimal(),
        habsStatus = habsStatus(Hab.fromFarm(farm), Duration.ZERO),
        transportBottleneck = when {
            eggIncrease(Hab.fromFarm(farm), constants) >= constants.transportRate -> Duration.ZERO
            else -> null
        }
    )

    val population: BigDecimal get() = habs.sumByBigDecimal(Hab::population)

    override fun toString(): String = "${this::class.simpleName}(" +
            "population=${habs.sumByBigDecimal(Hab::population).asIllions()}, " +
            "capacity=${habs.sumByBigDecimal(Hab::capacity).asIllions()}, " +
            "eggsLaid=${eggsLaid.asIllions()}, " +
            "habBottleneckReached=${habsStatus}, " +
            "transportBottleneckReached=${transportBottleneck?.asDaysHoursAndMinutes()}" +
            ")"
}