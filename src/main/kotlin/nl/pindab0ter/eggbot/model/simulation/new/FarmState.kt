package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.asDaysHoursAndMinutes
import nl.pindab0ter.eggbot.helpers.asIllions
import nl.pindab0ter.eggbot.helpers.eggIncrease
import nl.pindab0ter.eggbot.helpers.sumByBigDecimal
import org.joda.time.Duration
import java.math.BigDecimal

data class FarmState(
    val habs: List<Hab>,
    val eggsLaid: BigDecimal = BigDecimal.ZERO,
    val constants: Constants,
    val habBottleneck: Duration? = null,
    val transportBottleneck: Duration? = null,
) {
    constructor(farm: Backup.Simulation, constants: Constants) : this(
        habs = Hab.fromFarm(farm),
        eggsLaid = farm.eggsLaid.toBigDecimal(),
        constants = constants,
    ) {
        copy(
            habBottleneck = when {
                habs.sumByBigDecimal(Hab::population) >= habs.sumByBigDecimal(Hab::capacity) -> Duration.ZERO
                else -> null
            },
            transportBottleneck = when {
                eggIncrease(habs, constants) >= constants.transportRate -> Duration.ZERO
                else -> null
            }
        )
    }

    override fun toString(): String = "${this::class.simpleName}(" +
            "population=${habs.sumByBigDecimal(Hab::population).asIllions()}, " +
            "capacity=${habs.sumByBigDecimal(Hab::capacity).asIllions()}, " +
            "eggsLaid=${eggsLaid.asIllions()}, " +
            "habBottleneckReached=${habBottleneck?.asDaysHoursAndMinutes()}, " +
            "transportBottleneckReached=${transportBottleneck?.asDaysHoursAndMinutes()}" +
            ")"
}