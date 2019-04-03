package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.daysHoursAndMinutes
import nl.pindab0ter.eggbot.formattedName
import nl.pindab0ter.eggbot.network.AuxBrain
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

    override fun execute(event: CommandEvent) {
        AuxBrain.getContracts { getContractsResponse ->
            val contracts = getContractsResponse.contractsList
                .sortedBy { it.expirationTime }

            if (contracts.isNotEmpty()) event.reply(StringBuilder(ACTIVE_CONTRACTS).appendln().apply {
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
            else event.replyWarning(ContractIDs.NO_ACTIVE_CONTRACTS)

        }
    }
}

