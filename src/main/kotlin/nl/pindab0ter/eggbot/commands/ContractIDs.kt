package nl.pindab0ter.eggbot.commands

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.httpGet
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.daysHoursAndMinutes
import nl.pindab0ter.eggbot.formattedName
import nl.pindab0ter.eggbot.network.GET_CONTRACTS_URL
import nl.pindab0ter.eggbot.network.base64Decoded
import nl.pindab0ter.eggbot.toDateTime
import org.joda.time.DateTime
import org.joda.time.Duration

object ContractIDs : Command() {
    private const val ACTIVE_CONTRACTS = "IDs for currently active contracts:"
    private const val NO_ACTIVE_CONTRACTS = "There are currently no active contracts"

    init {
        name = "contractids"
        help = "Shows the IDs of the currently active contracts"
        guildOnly = false
    }


    // TODO: Refactor and handle failure
    override fun execute(event: CommandEvent) {
        val contracts = EggInc.GetContractsResponse
            .parseFrom(GET_CONTRACTS_URL.httpGet().response().second.body().base64Decoded())
            .contractsList
            .sortedBy { it.expirationTime }

        when {
            contracts.isNotEmpty() -> event.reply(StringBuilder(ACTIVE_CONTRACTS).appendln().apply {
                contracts.forEach { contract ->
                    append("**`${contract.identifier}`**: ")
                    append("${contract.name} (${contract.egg.formattedName})")
                    append(", valid for ")
                    append(
                        daysHoursAndMinutes.print(
                            Duration(
                                DateTime.now(),
                                contract.expirationTime.toDateTime()
                            ).toPeriod().normalizedStandard()
                        )
                    )
                    appendln()
                }
            }.toString())
            else -> event.replyWarning(ContractIDs.NO_ACTIVE_CONTRACTS)
        }
    }
}

