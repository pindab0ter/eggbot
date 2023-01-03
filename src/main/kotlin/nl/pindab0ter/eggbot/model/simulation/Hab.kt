package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import com.auxbrain.ei.CoopStatus
import nl.pindab0ter.eggbot.model.auxbrain.capacity
import nl.pindab0ter.eggbot.model.auxbrain.habCapacityMultiplier
import nl.pindab0ter.eggbot.model.auxbrain.habCapacityMultiplierFor
import java.math.BigDecimal

data class Hab(
    val population: BigDecimal,
    val capacity: BigDecimal,
) {
    companion object {
        fun fromFarm(farm: Backup.Farm, backup: Backup): List<Hab> = (0..3).map { index ->
            Hab(
                population = farm.habPopulations[index].toBigDecimal(),
                capacity = farm.habLevels[index].capacity.multiply(backup.habCapacityMultiplierFor(farm))
            )
        }
    }
}

val CoopStatus.FarmInfo.habs
    get() = (0..3).map { index ->
        Hab(
            population = habPopulations[index].toBigDecimal(),
            capacity = habLevels[index].capacity.multiply(habCapacityMultiplier)
        )
    }