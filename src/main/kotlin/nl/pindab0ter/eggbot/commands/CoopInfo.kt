package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.database.Contract
import nl.pindab0ter.eggbot.network.AuxBrain.getCoopStatus
import org.jetbrains.exposed.sql.transactions.transaction

object CoopInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop"
        aliases = arrayOf("coopinfo", "ci", "coop-info")
        arguments = "<co-op id> <contract id>"
        help = "Shows the progress of a specific co-op."
        // category = ContractsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        when {
            event.arguments.size < 2 -> missingArguments.let {
                event.replyWarning(it)
                log.trace { it }
                return
            }
            event.arguments.size > 2 -> tooManyArguments.let {
                event.replyWarning(it)
                log.trace { it }
                return
            }
        }

        getCoopStatus(event.arguments[0], event.arguments[1]).let getCoopStatus@{ (status, _) ->
            if (status == null || !status.isInitialized) "Could not get co-op status. Are the `co-op id` and `contract id` correct?.".let {
                event.replyWarning(it)
                log.trace { it }
                return@getCoopStatus
            }

            transaction {
                Contract.getOrNew(status.contractIdentifier)?.let { contract ->
                    Messages.coopStatus(contract, status).let { message ->
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
    }
}
