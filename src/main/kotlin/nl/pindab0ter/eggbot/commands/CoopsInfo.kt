package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.menu.OrderedMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.jda.commandClient
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit.MINUTES

object CoopsInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coops"
        aliases = arrayOf("coopsinfo", "coops-info", "co-ops", "co-ops-info")
        arguments = "<contract id>"
        help = "Shows info on all co-ops for the chosen contract."
        // category = ContractsCategory
        hidden = true
        guildOnly = false
    }

    val builder = OrderedMenu.Builder()
        .allowTextInput(true)
        .useNumbers()
        .setEventWaiter(EggBot.eventWaiter)
        .useCancelButton(true)
        .setTimeout(1, MINUTES)

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        // If the admin role is defined check whether the author has at least that role or is the guild owner
        if (Config.adminRole != null && event.author.mutualGuilds.none { guild ->
                guild.getMember(event.author).let { author ->
                    author.isOwner || author.user.id == Config.ownerId || author.roles.any { memberRole ->
                        guild.getRolesByName(Config.adminRole, true)
                            .any { adminRole -> memberRole.position >= adminRole.position }
                    }
                }
            }) "You must have at least a role called `${Config.adminRole}` to use that!".let {
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
                    append("`${result.coopId}`: ✗ No active co-op found, remove using ")
                    append("`${commandClient.prefix}${CoopRemove.name} ${result.contractId} ${result.coopId}`?")
                }.toString()
                is Empty -> StringBuilder().apply {
                    append("`${result.coopStatus.coopIdentifier}`: ✗ Empty co-op, remove using ")
                    append("`${commandClient.prefix}${CoopRemove.name} ${result.coopStatus.contractIdentifier} ${result.coopStatus.coopIdentifier}`?")
                }.toString()
                is InProgress -> when {
                    result.simulation.projectedToFinish() -> StringBuilder().apply {
                        append("`${result.simulation.coopId}`: ✓ Will finish ")
                        append("(${result.simulation.projectedTimeToFinalGoal()?.asDayHoursAndMinutes(true)}/")
                        append("${result.simulation.timeRemaining.asDayHoursAndMinutes(true)})")
                    }
                    else -> StringBuilder().apply {
                        append("`${result.simulation.coopId}`: ✗ Won't finish ")
                        append("(${result.simulation.projectedTimeToFinalGoal()?.asDayHoursAndMinutes(true)}/")
                        append("${result.simulation.timeRemaining.asDayHoursAndMinutes(true)})")
                    }
                }
                is Finished -> "`${result.coopStatus.coopIdentifier}`: ✓ Finished"
            }
        }}")
    }
}
