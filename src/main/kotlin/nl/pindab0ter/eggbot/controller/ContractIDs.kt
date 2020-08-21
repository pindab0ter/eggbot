package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.Contract
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.EggBot.eggsToEmotes
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.helpers.asDaysHoursAndMinutes
import nl.pindab0ter.eggbot.helpers.toDateTime
import org.joda.time.DateTime
import org.joda.time.Duration

object ContractIDs : EggBotCommand() {

    init {
        category = ContractsCategory
        name = "contract-ids"
        aliases = arrayOf("ids")
        help = "Shows the IDs of the currently active contracts"
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        AuxBrain.getPeriodicals { periodicalsResponse ->
            val (soloContracts, coopContracts) = periodicalsResponse.contracts!!.contracts
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
    private fun List<Contract>.printContracts(): String = StringBuilder().apply {
        this@printContracts.forEach { contract ->
            append("**`${contract.id}`**: ")
            append("${contract.name} ")
            append(eggsToEmotes[contract.egg]?.asMention ?: "(${contract.egg.name})")
            append(", valid for ")
            append(
                Duration(DateTime.now(), contract.expirationTime.toDateTime())
                    .toPeriod()
                    .normalizedStandard()
                    .asDaysHoursAndMinutes()
            )
            appendLine()
        }
    }.toString()
}

