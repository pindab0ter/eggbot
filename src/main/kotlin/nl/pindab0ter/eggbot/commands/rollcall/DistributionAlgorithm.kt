package nl.pindab0ter.eggbot.commands.rollcall

import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.database.Contract
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

abstract class DistributionAlgorithm {
    abstract fun createRollCall(farmers: List<Farmer>, contract: Contract): List<Coop>
    internal fun createCoops(farmers: List<Farmer>, contract: Contract): List<Coop> = transaction {
        List(((farmers.count() * 1.2) / contract.maxCoopSize).toInt() + 1) { index ->
            Coop.new {
                this.contract = contract
                this.name = Config.coopIncrementChar.plus(index).toString() +
                        Config.coopName +
                        contract.maxCoopSize
            }
        }
    }
}
