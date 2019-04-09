package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction

object Register : Command() {
    init {
        name = "register"
        arguments = "<in-game name> <in-game id>"
        help = "Register on this server with your in-game name and in-game ID."
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
            val farmers = Farmer.all().toList()
            val farmerInfo = AuxBrain.firstContact(registrant.inGameId).backup

            // Check if the in-game ID is valid
            if (farmerInfo.userid.isBlank()) {
                event.replyError(
                    "No information was found for the given Egg, Inc. ID. Did you make a typo?"
                )
                return@transaction
            }

            // Check if the in-game name matches with the in-game name belonging to the in-game ID's account
            if (registrant.inGameId != farmerInfo.userid || registrant.inGameName.toLowerCase() != farmerInfo.name.toLowerCase()) {
                event.replyError(
                    "The given username (`${registrant.inGameName}`) does not match the username for that Egg, Inc. ID (`${farmerInfo.name}`)"
                )
                return@transaction
            }

            // Check if the Discord user is already known, otherwise create a new user
            val discordUser: DiscordUser = DiscordUser.find {
                DiscordUsers.id eq registrant.discordId
            }.firstOrNull() ?: DiscordUser.new(registrant.discordId) {
                discordTag = registrant.discordTag
            }

            // Check if this Discord user hasn't already registered that in-game name
            if (discordUser.farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName }) {
                event.replyWarning(
                    "You are already registered with the in-game names: `${discordUser.farmers.joinToString("`, `") { it.inGameName }}`."
                )
                return@transaction
            }

            // Check if someone else hasn't already registered that in-game name
            if (farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName }) {
                event.replyWarning(
                    "Someone else has already registered the in-game name `${registrant.inGameName}`."
                )
                return@transaction
            }

            // Add the new in-game name
            Farmer.new(registrant.inGameId) {
                discordId = discordUser
                inGameName = farmerInfo.name.replace('`', '\'')
                soulEggs = farmerInfo.data.soulEggs
                prophecyEggs = farmerInfo.data.prophecyEggs
                soulBonus = farmerInfo.data.epicResearchList.find { it.id == "soul_eggs" }!!.level
                prophecyBonus = farmerInfo.data.epicResearchList.find { it.id == "prophecy_bonus" }!!.level
            }

            // Finally confirm the registration
            if (discordUser.farmers.filterNot { it.inGameId == registrant.inGameId }.none()) {
                event.replySuccess(
                    "You have been registered with the in-game name `${farmerInfo.name}`, welcome!"
                )
            } else {
                event.replySuccess(
                    "You are now registered with the in-game name `${farmerInfo.name}`, as well as `${discordUser.farmers
                        .filterNot { it.inGameId == registrant.inGameId }
                        .joinToString(" `, ` ") { it.inGameName }}`!"
                )
            }
        }
    }
}
