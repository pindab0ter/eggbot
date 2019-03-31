package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.database.Farmers
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

object Register : Command() {
    init {
        name = "register"
        arguments = "<in-game name>"
        help = "Register on this server with your in-game name"
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.args.isBlank()) {
            event.replyWarning("Missing in-game name argument. See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }

        val newDiscordTag = event.author.asTag
        val newInGameName = event.arguments[0]

        transaction {
            Farmer.find { (Farmers.id eq newDiscordTag) or (Farmers.inGameName eq newInGameName) }.firstOrNull()
                ?.let { farmer ->
                    if (farmer.discordTag.value != newDiscordTag)
                        event.replyWarning("Someone else has already registered as `${farmer.inGameName}`.")
                    else
                        event.replyWarning("You are already registered as `${farmer.inGameName}`.")
                } ?: Farmer.new(newDiscordTag) {
                inGameName = newInGameName
            }.apply {
                event.replySuccess("You have registered as `$newInGameName`, welcome!")
            }
        }
    }
}