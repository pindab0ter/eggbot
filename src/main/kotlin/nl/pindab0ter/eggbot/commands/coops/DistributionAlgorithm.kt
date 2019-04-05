package nl.pindab0ter.eggbot.commands.coops

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.database.Coop

interface DistributionAlgorithm {
    fun createCoops(contract: EggInc.Contract): List<Coop>
}