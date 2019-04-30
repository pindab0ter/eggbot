package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.missingArguments
import nl.pindab0ter.eggbot.tooManyArguments

object CoopInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop"
        aliases = arrayOf("coopinfo", "ci", "coop-info")
        arguments = "<co-op ID> <contract ID>"
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
    }
}

