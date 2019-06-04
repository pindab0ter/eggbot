package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.auxbrain.CoopContractSimulation
import nl.pindab0ter.eggbot.network.AuxBrain.getCoopStatus

@Suppress("FoldInitializerAndIfToElvis")
object CoopInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop"
        aliases = arrayOf("coopinfo", "ci", "coop-info")
        arguments = "<contract id> <co-op id>"
        help = "Shows the progress of a specific co-op."
        // category = ContractsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        when {
            event.arguments.size < 2 -> missingArguments.let {
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

        val (contractId, coopId) = event.arguments

        getCoopStatus(contractId, coopId).let getCoopStatus@{ (status, _) ->
            if (status == null || !status.isInitialized) "Could not get co-op status. Are the `contract id` and `co-op id` correct?.".let {
                event.replyWarning(it)
                log.debug { it }
                return@getCoopStatus
            }

            val coopContractSimulation = CoopContractSimulation(status)

            if (coopContractSimulation == null) "Could not get co-op status. Are the `contract id` and `co-op id` correct?.".let {
                event.replyWarning(it)
                log.debug { it }
                return@getCoopStatus
            }

            Messages.coopStatus(coopContractSimulation).let { message ->
                if (event.channel.id == Config.botCommandsChannel) {
                    event.reply(message)
                } else {
                    event.replyInDm(message)
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
            }
        }
    }
}

