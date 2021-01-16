package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.model.auxbrain.capacity
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
                capacity = farm.habs[index].capacity.multiply(backup.habCapacityMultiplierFor(farm))
            )
        }
    }
}