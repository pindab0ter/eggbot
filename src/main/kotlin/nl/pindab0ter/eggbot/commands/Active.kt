package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction

object Active : Command() {
    init {
        name = "active"
        help = "Set yourself as active."
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val discordUser = transaction { DiscordUser.findById(event.author.id) }

        if (discordUser == null) {
            event.replyWarning("You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.")
            return
        }

        when {
            discordUser.isActive -> {
                event.reply("You are already active.")
                return
            }
            else -> {
                transaction { discordUser.inactiveUntil = null }
                event.replySuccess("You are now active again.")
                return
            }
        }
    }
}
