package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.ProgressBar
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.simulation.old.CoopContractSimulation
import nl.pindab0ter.eggbot.model.simulation.old.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.helpers.NumberFormatter.*
import nl.pindab0ter.eggbot.model.ProgressBar.WhenDone
import org.jetbrains.exposed.sql.transactions.transaction

object CoopsInfo : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    init {
        category = ContractsCategory
        name = "coops"
        help = "Shows info on all known co-ops for the specified contract."
        parameters = listOf(contractIdOption)
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val message = event.channel.sendMessage("Looking for co-ops…").complete()

        val contractId = parameters.getString(CONTRACT_ID)
        val coops = transaction {
            Coop.find { Coops.contract eq contractId }.toList().sortedBy { it.name }
        }
        val progressBar = ProgressBar(coops.size, message, WhenDone.STOP_IMMEDIATELY)
        // Replace with mapAsync if running on a multi threaded machine.
        val results = coops.mapIndexed { i, coop ->
            CoopContractSimulation.Factory(coop.contract, coop.name).also {
                progressBar.update(i + 1)
            }
        }
        if (results.isEmpty()) "Could not find any co-ops for contract id `$contractId`.\nIs `contract id` correct and are there registered teams?".let {
            message.delete().complete()
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contract = AuxBrain.getPeriodicals()?.contracts?.contracts?.find { it.id == contractId }!!

        val resultRows = results.map { result ->
            when (result) {
                is NotFound -> ResultRow(result.coopId, "🟡", "Waiting for starter")
                is Abandoned -> ResultRow(result.coopStatus.coopId, "🔴", "Abandoned")
                is InProgress -> when {
                    result.simulation.willFinish -> ResultRow(
                        result.simulation.coopId,
                        "🟢",
                        "On track",
                        result.simulation.eggspected.asIllions(),
                        result.simulation.coopStatus.contributors.count().toString()
                    )
                    else -> ResultRow(
                        result.simulation.coopId,
                        "🔴",
                        "Not on track",
                        result.simulation.eggspected.asIllions(),
                        result.simulation.coopStatus.contributors.count().toString()
                    )
                }
                is Failed -> ResultRow(result.coopStatus.coopId, "🔴", "Failed")
                is Finished -> ResultRow(result.coopStatus.coopId, "🏁", "Finished")
            }
        }
        val longestName = resultRows.map { it.name }.plus("Name").maxByOrNull { it.length }!!
        val longestStatusMessage = resultRows.map { it.statusMessage }.plus("Status").maxByOrNull { it.length }!!
        val longestEggspected = resultRows.map { it.eggspected }.plus("Eggspected").maxByOrNull { it.length }!!
        val longestMembers = resultRows.map { it.members }.maxByOrNull { it.length }!!

        StringBuilder("`${guild.name}` vs _${contractId}_:\n").apply {

            // region Basic info

            appendLine()
            appendLine("__🗒️ **Basic info**__ ```")
            appendLine("Contract:         ${contract.name}")
            appendLine("Final goal:       ${contract.finalGoal.asIllions(OPTIONAL_DECIMALS)}")
            appendLine("Time to complete: ${contract.lengthSeconds.toDuration().asDaysHoursAndMinutes(true)}")
            appendLine("Max size:         ${contract.maxCoopSize} farmers")

            appendLine("```")

            // endregion Basic info

            // region Table

            appendLine("__**🤝 Co-ops**__```")

            // region Table header

            append("Name ")
            appendPaddingCharacters("Name", longestName)
            append("│ 🚥 │ Status ")
            appendPaddingCharacters("Status", longestStatusMessage)
            append("| Eggspected ")
            appendPaddingCharacters("Eggspected", longestEggspected)
            append("| ")
            appendPaddingCharacters("", longestMembers, "#")
            append("/${contract.maxCoopSize}")
            appendLine()

            appendPaddingCharacters("", longestName, "═")
            append("═╪═➖═╪═")
            appendPaddingCharacters("", longestStatusMessage, "═")
            append("═╪═")
            appendPaddingCharacters("", longestEggspected, "═")
            append("═╪══")
            appendPaddingCharacters("", longestMembers, "═")
            appendPaddingCharacters("", contract.maxCoopSize, "═")
            appendLine()

            // endregion Table header

            // region Table body

            resultRows.forEach { row ->
                append(row.name)
                appendPaddingCharacters(row.name, longestName)
                append(" │ ${row.statusEmoji} │ ${row.statusMessage}")
                appendPaddingCharacters(row.statusMessage, longestStatusMessage)
                append(" │ ")
                appendPaddingCharacters(row.eggspected, longestEggspected)
                append(row.eggspected)
                append(" │ ")
                appendPaddingCharacters(row.members, longestMembers)
                if (row.members.isNotBlank()) append("${row.members}/${contract.maxCoopSize}")
                else {
                    appendPaddingCharacters("", longestMembers)
                    append(" ")
                    appendPaddingCharacters("", contract.maxCoopSize)
                }
                appendLine()
            }

            // endregion Table body

            appendLine("```")

            // endregion Table
        }.toString().splitCodeBlock().let { messages ->
            message.delete().queue()
            messages.forEach { message -> event.reply(message) }
        }
    }

    class ResultRow(
        val name: String,
        val statusEmoji: String,
        val statusMessage: String,
        val eggspected: String = "",
        val members: String = ""
    )
}
