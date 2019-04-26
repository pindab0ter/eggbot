package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.commands.categories.UsersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.monthAndDay
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object Inactive : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "inactive"
        help = "Set yourself as inactive for `[days]` days or check whether you're inactive if no argument is given."
        arguments = "[days]"
        category = UsersCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.arguments.count() > 1) "Too many arguments. See `${event.client.textualPrefix}${event.client.helpWord}` for more information.".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        val discordUser = transaction { DiscordUser.findById(event.author.id) }

        @Suppress("FoldInitializerAndIfToElvis")
        if (discordUser == null) "You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        when (val argument = event.arguments.firstOrNull()) {
            // If no arguments are given
            null -> when {
                discordUser.isActive -> {
                    event.reply("You are not inactive.")
                    return
                }
                else -> {
                    event.reply("You are set to be inactive until **${monthAndDay.print(discordUser.inactiveUntil)}**.")
                    return
                }
            }
            // If an argument is given
            else -> when (val days = argument.toIntOrNull()) {
                null -> "Could not make sense of `$argument`, please enter a number of days.".let {
                    event.replyWarning(it)
                    log.trace { it }
                    return
                }
                in Int.MIN_VALUE..0 -> "The number of days must be positive.".let {
                    event.replyWarning(it)
                    log.trace { it }
                    return
                }
                else -> DateTime.now().plusDays(days).let { inactiveUntil ->
                    log.info { "User ${discordUser.discordTag} will be inactive for $days days" }
                    transaction { discordUser.inactiveUntil = inactiveUntil }
                    event.replySuccess("You will be inactive until **${monthAndDay.print(inactiveUntil)}** or until you use `${event.client.textualPrefix}${Active.name}`.")
                }
            }
        }
    }
}
