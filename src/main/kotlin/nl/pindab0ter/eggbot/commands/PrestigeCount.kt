package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.formatIllions
import nl.pindab0ter.eggbot.missingArguments
import nl.pindab0ter.eggbot.tooManyArguments
import kotlin.math.E
import kotlin.math.ceil
import kotlin.math.log

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

        val soulEggs = event.arguments.getOrNull(0)?.replace(Regex("""\D"""), "")?.toDoubleOrNull()
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

        val requiredPrestiges = ceil(
            when {
                soulEggs < 100000000000000 -> log((soulEggs / 4) / 14600000000, E) / 0.0744
                soulEggs < 930000000000000 -> log((soulEggs / 4) / 19000000000, E) / 0.0716
                else -> (soulEggs / 4 - 40000000000) / 1758889813535
            }
        ).toInt()

        event.reply("You need **$requiredPrestiges prestiges** to be in the clear for `${soulEggs.formatIllions()} SE`.")
    }
}
