package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.utilities.*
import nl.pindab0ter.eggbot.utilities.ProgressBar.WhenDone
import org.jetbrains.exposed.sql.transactions.transaction

object CoopsInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coops"
        aliases = arrayOf("coops-info")
        arguments = "<contract id>"
        help = "Shows info on all known co-ops for the specified contract."
        category = ContractsCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {

        (checkPrerequisites(
            event,
            minArguments = 1,
            maxArguments = 1
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val message = event.channel.sendMessage("Looking for co-ops…").complete()

        val contractId = event.arguments.first()
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

        val contract = AuxBrain.getPeriodicals()?.contracts?.contractsList?.find { it.id == contractId }!!

        val resultRows = results.map { result ->
            when (result) {
                is NotFound -> ResultRow(result.coopId, "🟡", "Waiting for starter")
                is Abandoned -> ResultRow(result.coopStatus.coopId, "🔴", "Abandoned")
                is InProgress -> when {
                    result.simulation.willFinish -> ResultRow(
                        result.simulation.coopId,
                        "🟢",
                        "On track",
                        result.simulation.eggspected.formatIllions(),
                        result.simulation.coopStatus.contributorsCount.toString()
                    )
                    else -> ResultRow(
                        result.simulation.coopId,
                        "🔴",
                        "Not on track",
                        result.simulation.eggspected.formatIllions(),
                        result.simulation.coopStatus.contributorsCount.toString()
                    )
                }
                is Failed -> ResultRow(result.coopStatus.coopId, "🔴", "Failed")
                is Finished -> ResultRow(result.coopStatus.coopId, "🏁", "Finished")
            }
        }
        val longestName = resultRows.map { it.name }.plus("Name").maxBy { it.length }!!
        val longestStatusMessage = resultRows.map { it.statusMessage }.plus("Status").maxBy { it.length }!!
        val longestEggspected = resultRows.map { it.eggspected }.plus("Eggspected").maxBy { it.length }!!
        val longestMembers = resultRows.map { it.members }.maxBy { it.length }!!

        StringBuilder("`${EggBot.guild.name}` vs _${contractId}_:\n").apply {

            // region Basic info

            appendln()
            appendln("__🗒️ **Basic info**:__ ```")
            appendln("Contract:         ${contract.name}")
            appendln("Final goal:       ${contract.finalGoal.formatIllions(true)}")
            appendln("Time to complete: ${contract.lengthSeconds.toDuration().asDaysHoursAndMinutes(true)}")
            appendln("Max size:         ${contract.maxCoopSize} farmers")

            appendln("```")

            // endregion Basic info

            // region Table

            appendln("__**🤝 Co-ops:**__```")

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
            appendln()

            appendPaddingCharacters("", longestName, "═")
            append("═╪═➖═╪═")
            appendPaddingCharacters("", longestStatusMessage, "═")
            append("═╪═")
            appendPaddingCharacters("", longestEggspected, "═")
            append("═╪══")
            appendPaddingCharacters("", longestMembers, "═")
            appendPaddingCharacters("", contract.maxCoopSize, "═")
            appendln()

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
                appendln()
            }

            // endregion Table body

            appendln("```")

            // endregion Table
        }.toString().let { messageBody ->
            message.delete().queue()
            event.reply(messageBody)
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
