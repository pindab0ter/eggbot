package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory

object PrestigeCount : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "prestige-count"
        aliases = arrayOf("prestiges", "pc", "prestigecount")
        help = "Calculates how many prestiges you need to get out of backup-bug territory."
        arguments = "<soul eggs>"
        category = FarmersCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.arguments.isEmpty()) missingArguments.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        if (event.arguments.size > 1) tooManyArguments.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val soulEggs = event.arguments.getOrNull(0)?.toDoubleOrNull()
            ?: "Could not parse the first argument (`${event.arguments.getOrNull(0)}`) as a number.".let {
                event.replyWarning(it)
                log.debug { it }
                return
            }

        if (soulEggs < 0)
            "Amount of soul eggs must be positive.".let {
                event.replyWarning(it)
                log.debug { it }
                return
            }

        val requiredPrestiges = calculatePrestigesFor(soulEggs)

        event.reply("You need **$requiredPrestiges prestiges** to be in the clear for `${soulEggs.formatIllions()} SE`.")
    }
}
