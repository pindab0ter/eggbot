package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.InGameName
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

        val tag = event.author.asTag
        val name = event.arguments[0]

        transaction {
            val inGameNames = InGameName.all().toList().map { it.inGameName }
            val users = DiscordUser.all().toList()

            // Check if the Discord user is already known
            val user = users.find { it.discordTag.value == tag }?.let { user ->
                when {

                    // Check if this Discord user hasn't already registered that in-game name
                    user.inGameNames.any { it == name } -> {
                        event.replyWarning(
                            "You are already registered with the in-game names:\n`${user.inGameNames.joinToString("`, `")}`."
                        )
                        return@transaction
                    }

                    // Check if someone else hasn't already registered that in-game name
                    inGameNames.subtract(user.inGameNames).any { it == name } -> {
                        event.replyWarning(
                            "Someone else has already registered the in-game name `$name`."
                        )
                        return@transaction
                    }

                    // Otherwise use the known Discord user
                    else -> user
                }
            } ?: {
                // Otherwise, register the new Discord user
                DiscordUser.new(tag) {}
            }()

            // Add the new in-game name
            InGameName.new {
                discordTag = user
                inGameName = name
            }

            // Finally confirm the registration
            if (user.inGameNames.isEmpty()) {
                event.replySuccess(
                    "You have been registered with the in-game name `$name`, welcome!"
                )
            } else {
                event.replySuccess(
                    "You are now registered with the in-game name `$name`, as well as `${inGameNames.joinToString("`, `")}`!"
                )
            }
        }
    }
}
