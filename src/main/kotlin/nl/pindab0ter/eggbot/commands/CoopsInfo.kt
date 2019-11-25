package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.sql.transactions.transaction

object CoopsInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coops"
        aliases = arrayOf("coopsinfo", "coops-info", "co-ops", "co-ops-info")
        arguments = "<contract id>"
        help = "Shows info on all co-ops for the specified contract."
        category = AdminCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            minArguments = 1,
            maxArguments = 1
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractId = event.arguments.first()
        val coops = transaction {
            Coop.find { Coops.contract eq contractId }.toList().sortedBy { it.name }
        }
        val results = runBlocking(Dispatchers.IO) {
            coops.asyncMap { coop ->
                CoopContractSimulation.Factory(coop.contract, coop.name)
            }
        }
        if (results.isEmpty()) "Could not find any co-ops for contract id `$contractId`.\nIs `contract id` correct and are there registered teams?".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val eggspecteds = results
            .filterIsInstance<InProgress>()
            .map { it.simulation.eggspected.formatIllions() }

        val contract = AuxBrain.getPeriodicals()?.contracts?.contractsList?.find { it.id == contractId }!!
        val longestCoopName = coops.map { it.name }.plus("Time to complete").maxBy { it.length }!!

        log.debug { longestCoopName }

        StringBuilder("`${EggBot.guild.name}` vs _${contractId}_:\n").apply {
            appendln()
            appendln("__🗒️ **Basic info**:__ ```")

            append("Contract: ")
            appendPaddingCharacters("Contract", longestCoopName)
            append(contract.name)
            appendln()

            append("Final goal: ")
            appendPaddingCharacters("Final goal", longestCoopName)
            append(contract.finalGoal.formatIllions(true))
            appendln()

            append("Time to complete: ")
            appendPaddingCharacters("Time to complete", longestCoopName)
            append(contract.lengthSeconds.toDuration().asDaysHoursAndMinutes(true))
            appendln()

            append("Max size: ")
            appendPaddingCharacters("Max size", longestCoopName)
            append("${contract.maxCoopSize} farmers")
            appendln()

            appendln("```")

            appendln("__**🤝 Co-ops:**__```")
            results.forEach { result ->
                when (result) {
                    is NotFound -> {
                        append("${result.coopId}: ")
                        appendPaddingCharacters(result.coopId, longestCoopName)
                        // TODO: Tag starter and/or leader
                        append("🟠 Waiting for starter")
                    }
                    is Abandoned -> {
                        append("${result.coopStatus.coopId}: ")
                        appendPaddingCharacters(result.coopStatus.coopId, longestCoopName)
                        append("🔴 Abandoned")
                    }
                    is InProgress -> {
                        append("${result.simulation.coopId}: ")
                        appendPaddingCharacters(result.simulation.coopId, longestCoopName)
                        when {
                            result.simulation.willFinish -> {
                                append("🟠 On track ")
                                if (results.filterIsInstance<InProgress>().any { !it.simulation.willFinish }) append("    ")
                                append("(")
                            }
                            else -> {
                                append("🔴 Not on track (")
                            }
                        }
                        appendPaddingCharacters(result.simulation.eggspected.formatIllions(), eggspecteds)
                        append("${result.simulation.eggspected.formatIllions()}, ")
                        appendPaddingCharacters(
                            result.simulation.farms.size,
                            results.filterIsInstance<InProgress>().map { it.simulation.farms.size }
                        )
                        append("${result.simulation.farms.size}/${result.simulation.maxCoopSize} farmers)")
                    }
                    is Failed -> {
                        append("${result.coopStatus.coopId}: ")
                        appendPaddingCharacters(result.coopStatus.coopId, longestCoopName)
                        append("🔴 Failed")
                    }
                    is Finished -> {
                        append("${result.coopStatus.coopId}: ")
                        appendPaddingCharacters(result.coopStatus.coopId, longestCoopName)
                        append("🟢 Finished")
                    }
                }
                appendln()
            }
            appendln("```")
        }.toString().let { string ->
            event.reply(string)
        }
    }
}
