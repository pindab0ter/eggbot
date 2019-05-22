package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import com.auxbrain.ei.EggInc.Backup
import com.auxbrain.ei.EggInc.FarmType.HOME
import com.auxbrain.ei.EggInc.HabLevel.NO_HAB
import nl.pindab0ter.eggbot.habPopulation
import nl.pindab0ter.eggbot.sum
import nl.pindab0ter.eggbot.toDuration
import org.joda.time.Duration
import java.math.RoundingMode

open class HomeSimulation(backup: Backup) : Simulation(backup) {
    override val farm: EggInc.Simulation = backup.farmsList.find { it.farmType == HOME }!!

    val timeToFullHabs: Duration by lazy {
        farm.habsList
            .mapIndexed { index, hab -> hab.maxCapacity - farm.habPopulation[index] }
            .map { roomToGrow ->
                roomToGrow.divide(internalHatcheryRatePerSecond, RoundingMode.HALF_UP).toLong().toDuration()
            }
            .sum()
            .dividedBy(farm.habsList.foldIndexed(0L) { index, acc, hab ->
                acc + if (hab.maxCapacity.toInt() == farm.habPopulationList[index] || hab == NO_HAB) 0L else 1L
            }.coerceAtLeast(1))
    }

    // TODO: Fix incorrect formula (mixing minutes/seconds or is calculation wrong?
    val timeToMaxShipping: Duration by lazy {
        ((shippingRatePerSecond / eggLayingRatePerSecond * population - population) / internalHatcheryRatePerSecond).toLong().toDuration()
    }
}
