package nl.pindab0ter.eggbot.commands

import com.github.kittinunf.fuel.Fuel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import nl.pindab0ter.eggbot.auxbrain.EggInc
import nl.pindab0ter.eggbot.database.Contract
import nl.pindab0ter.eggbot.network.GET_CONTRACTS_URL
import nl.pindab0ter.eggbot.network.base64Decoded
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Duration

object Contracts : Command {
    override val keyWord = "contracts"
    override val help = "$PREFIX$keyWord - Shows currently active contracts"

    override fun execute(event: MessageReceivedEvent) {
        val response = Fuel.get(GET_CONTRACTS_URL).response().second

        val contractsResponse = EggInc.GetContractsResponse.parseFrom(response.body().base64Decoded())
        contractsResponse.contractsList.forEach { contract ->
            transaction {
                Contract.new(contract.identifier) {
                    name = contract.name
                    description = contract.description
                    egg = contract.egg
                    coopAllowed = contract.coopAllow == 1
                    coopSize = contract.coopSize
                    validUntil = DateTime(contract.validUntil.toLong())
                    duration = Duration(contract.duration.toLong())
                }
            }
        }
    }
}
