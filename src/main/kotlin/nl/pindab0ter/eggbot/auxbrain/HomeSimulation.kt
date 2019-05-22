package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import com.auxbrain.ei.EggInc.Backup
import com.auxbrain.ei.EggInc.FarmType.HOME

open class HomeSimulation(backup: Backup) : Simulation(backup) {
    override val farm: EggInc.Simulation = backup.farmsList.find { it.farmType == HOME }!!
}
