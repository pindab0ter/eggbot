package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.jda.commandClient
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
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

        if (!hasPermission(event.author, Config.adminRole) && event.channelType == ChannelType.PRIVATE)
            "This command cannot be used in DMs. Please try again in a public channel.".let {
                event.replyError(it)
                log.debug { it }
                return
            }

        when {
            event.arguments.isEmpty() -> missingArguments.let {
                event.replyWarning(it)
                log.debug { it }
                return
            }
            event.arguments.size > 2 -> tooManyArguments.let {
                event.replyWarning(it)
                log.debug { it }
                return
            }
        }

        val contractId = event.arguments.first()
        val coops = runBlocking(Dispatchers.IO) {
            transaction {
                Coop.find { Coops.contract eq contractId }.toList()
            }.asyncMap { coop ->
                CoopContractSimulation.Factory(coop.contract, coop.name)
            }
        }

        if (coops.isEmpty()) "Could not find any co-ops for contract id `$contractId`.\nIs `contract id` correct and are there registered teams?".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        event.reply("Registered co-ops for `$contractId`:\n${coops.joinToString("\n") { result ->
            when (result) {
                is NotFound -> StringBuilder().apply {
                    append("`${result.coopId}`: ✗ Waiting for starter")
                }.toString()
                is Empty -> StringBuilder().apply {
                    append("`${result.coopStatus.coopIdentifier}`: ✗ Empty co-op, remove using ")
                    append("`${commandClient.prefix}${CoopRemove.name} ${result.coopStatus.contractIdentifier} ${result.coopStatus.coopIdentifier}`?")
                }.toString()
                is InProgress -> {
                    val progress = (result.simulation.timeRemaining / result.simulation.projectedTimeToFinalGoal()!!)
                        ?.asPercentage() ?: "error"
                    when {
                        result.simulation.projectedToFinish() -> StringBuilder().apply {
                            append("`${result.simulation.coopId}`: ✓ Will finish")
                            append("($progress)")
                        }
                        else -> StringBuilder().apply {
                            append("`${result.simulation.coopId}`: ✗ Won't finish")
                            append("($progress)")
                        }
                    }
                }
                is Finished -> "`${result.coopStatus.coopIdentifier}`: ✓ Finished"
            }
        }}")
    }
}
