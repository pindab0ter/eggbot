package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.commands.categories.ContractsCategory
import nl.pindab0ter.eggbot.network.AuxBrain.getCoopStatus
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.utilities.*

@Suppress("FoldInitializerAndIfToElvis")
object CoopInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop"
        aliases = arrayOf("coopinfo", "ci", "coop-info", "co-op", "co-op-info")
        arguments = "<contract id> <co-op id> [compact]"
        help = "Shows the progress of a specific co-op."
        category = ContractsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            minArguments = 2,
            maxArguments = 3
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractId: String = event.arguments[0]
        val coopId: String = event.arguments[1]
        val compact: Boolean = event.arguments.getOrNull(2)?.startsWith("c") == true

        getCoopStatus(contractId, coopId).let getCoopStatus@{ (status, _) ->
            if (status == null || !status.isInitialized) "Could not get co-op status. Are the `contract id` and `co-op id` correct?.".let {
                event.replyWarning(it)
                log.debug { it }
                return@getCoopStatus
            }

            Messages.coopStatus(
                CoopContractSimulation.Factory(status.contractId, status.coopId),
                compact
            ).let { messages ->
                if (event.channel.id == Config.botCommandsChannel) {
                    messages.forEach { message -> event.reply(message) }
                } else {
                    event.replyInDms(messages)
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
            }
        }
    }


}

