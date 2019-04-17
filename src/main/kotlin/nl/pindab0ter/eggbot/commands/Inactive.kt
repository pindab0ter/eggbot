package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.monthAndDay
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object Inactive : Command() {
    init {
        name = "inactive"
        help = "Set yourself as inactive for `[days]` days or check whether you're inactive if no argument is given."
        arguments = "[days]"
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.arguments.count() > 1) {
            event.replyWarning("Too many arguments. See `${event.client.textualPrefix}${event.client.helpWord}` for more information.")
            return
        }

        val discordUser = transaction { DiscordUser.findById(event.author.id) }

        if (discordUser == null) {
            event.replyWarning("You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.")
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
                null -> {
                    event.replyWarning("Could not make sense of `$argument`, please enter a number of days.")
                    return
                }
                else -> DateTime.now().plusDays(days).let { inactiveUntil ->
                    transaction { discordUser.inactiveUntil = inactiveUntil }
                    event.replySuccess("You will be inactive until **${monthAndDay.print(inactiveUntil)}** or until you use `${event.client.textualPrefix}${Active.name}`.")
                }
            }
        }
    }
}
