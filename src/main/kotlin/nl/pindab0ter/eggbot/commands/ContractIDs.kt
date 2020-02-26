package nl.pindab0ter.eggbot.commands

import com.auxbrain.ei.EggInc
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.ContractsCategory
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.asDaysHoursAndMinutes
import nl.pindab0ter.eggbot.utilities.formattedName
import nl.pindab0ter.eggbot.utilities.toDateTime
import org.joda.time.DateTime
import org.joda.time.Duration

object ContractIDs : Command() {

    init {
        name = "contract-ids"
        aliases = arrayOf("ids", "contractids", "current-ids", "currentids")
        help = "Shows the IDs of the currently active contracts"
        category = ContractsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        AuxBrain.getPeriodicals { periodicalsResponse ->
            val (soloContracts, coopContracts) = periodicalsResponse.contracts.contractsList
                .sortedBy { it.expirationTime }
                .groupBy { it.coopAllowed }
                .let { it[false].orEmpty() to it[true].orEmpty() }

            if (soloContracts.plus(coopContracts).isNotEmpty()) {
                event.reply(StringBuilder("IDs for currently active contracts:").apply {
                    if (soloContracts.isNotEmpty()) {
                        append("\n  __Solo contracts__:\n")
                        append(soloContracts.printContracts())
                    }
                    if (coopContracts.isNotEmpty()) {
                        append("\n  __Co-op contracts__:\n")
                        append(coopContracts.printContracts())
                    }
                }.toString())
            } else event.replyWarning("There are currently no active contracts")
        }
    }

    // TODO: Add info about requirements
    private fun List<EggInc.Contract>.printContracts(): String = StringBuilder().apply {
        this@printContracts.forEach { contract ->
            append("**`${contract.id}`**: ")
            append("${contract.name} ")
            append(Config.eggEmojiIds[contract.egg]
                ?.let { EggBot.jdaClient.getEmoteById(it)?.asMention }
                ?: "(${contract.egg.formattedName})"
            )
            append(", valid for ")
            append(
                Duration(DateTime.now(), contract.expirationTime.toDateTime())
                    .toPeriod()
                    .normalizedStandard()
                    .asDaysHoursAndMinutes()
            )
            appendln()
        }
    }.toString()
}

