package nl.pindab0ter.eggbot.commands.rollcall

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

abstract class DistributionAlgorithm {
    abstract fun createRollCall(farmers: List<Farmer>, contract: EggInc.Contract): List<Coop>
    internal fun createCoops(farmers: List<Farmer>, contract: EggInc.Contract): List<Coop> = transaction {
        List(((farmers.count() * 1.2) / contract.maxCoopSize).roundToInt()) { index ->
            Coop.new {
                this.contractId = contract.identifier
                this.name = Config.coopIncrementChar.plus(index).toString() +
                        Config.coopName +
                        contract.maxCoopSize
            }
        }
    }

}