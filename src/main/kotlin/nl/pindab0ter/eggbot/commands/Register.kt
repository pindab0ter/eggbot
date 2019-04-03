package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction

object Register : Command() {
    init {
        name = "register"
        arguments = "<in-game name> <in-game id>"
        help = "Register on this server with your in-game name and ID. In-game name must match the name associated with the ID."
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.arguments.count() < 2) {
            event.replyWarning("Missing argument(s). See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }
        if (event.arguments.count() > 2) {
            event.replyWarning("Too many arguments. See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }

        val registrant = object {
            val discordId = event.author.id
            val discordTag = event.author.asTag
            val inGameId = event.arguments[1]
            val inGameName = event.arguments[0]
        }

        transaction {
            val currentFarmers = Farmer.all().toList().map { it.inGameName }
            val currentDiscordUsers = DiscordUser.all().toList()
            val farmerInfo = AuxBrain.firstContact(registrant.inGameId).backup

            // Check if the in-game name matches with the in-game name belonging to the in-game ID's account
            if (registrant.inGameId != farmerInfo.userid || registrant.inGameName.toLowerCase() != farmerInfo.name.toLowerCase()) {
                event.replyError(
                    "The given username (`${registrant.inGameName}`) does not match the username for that Egg, Inc. ID (`${farmerInfo.name}`)"
                )
                return@transaction
            }

            // Check if the Discord user is already known
            val user = currentDiscordUsers.find { it.discordId == registrant.discordId }?.let { user ->
                when {

                    // Check if this Discord user hasn't already registered that in-game name
                    user.farmers.any { it.inGameName == registrant.inGameName } -> {
                        event.replyWarning(
                            "You are already registered with the in-game names:\n`${user.farmers.joinToString("`, `") { it.inGameName }}`."
                        )
                        return@transaction
                    }

                    // Check if someone else hasn't already registered that in-game name
                    currentFarmers.subtract(user.farmers).any { it == registrant.inGameName } -> {
                        event.replyWarning(
                            "Someone else has already registered the in-game name `${registrant.inGameName}`."
                        )
                        return@transaction
                    }

                    // Otherwise use the known Discord user
                    else -> user
                }
            } ?: {
                // Otherwise, register the new Discord user
                DiscordUser.new(registrant.discordId) {
                    discordTag = registrant.discordTag
                }
            }()

            // Add the new in-game name
            Farmer.new(registrant.inGameId) {
                discordId = user
                inGameName = farmerInfo.name
            }

            // Finally confirm the registration
            if (user.farmers.filterNot { it.inGameId == registrant.inGameId }.count() == 0) {
                event.replySuccess(
                    "You have been registered with the in-game name `${farmerInfo.name}`, welcome!"
                )
            } else {
                event.replySuccess(
                    "You are now registered with the in-game name `${farmerInfo.name}`, as well as `${currentFarmers.joinToString(
                        "`, `"
                    )}`!"
                )
            }
        }
    }
}
