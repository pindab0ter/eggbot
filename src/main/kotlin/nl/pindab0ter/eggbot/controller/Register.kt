package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import net.dv8tion.jda.api.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.api.entities.ChannelType.TEXT
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

object Register : EggBotCommand() {

    private const val IN_GAME_ID = "in-game id"
    private const val IN_GAME_NAME = "in-game name"

    init {
        category = FarmersCategory
        name = "register"
        help = "Register on this server with your in-game name and in-game ID. **DM only!**"
        registrationRequired = false
        parameters = listOf(
            UnflaggedOption(IN_GAME_ID)
                .setRequired(REQUIRED)
                .setHelp(
                    "Your in-game ID. Can be found by going to the Menu → Settings → More and looking in the" +
                            "bottom of that screen."
                ),
            UnflaggedOption(IN_GAME_NAME)
                .setRequired(REQUIRED)
                .setHelp(
                    "Your in-game name. If it contains spaces you must surround it with quotation marks " +
                            "(`\"name with spaces\"`). You can find your in-game name on iOS by going to your Settings" +
                            "app → Game Center → Nickname and on Android devices by going to the Play Games app → " +
                            "Edit profile → Gamer ID."
                )
        )
        sendTyping = false
        botPermissions = arrayOf(
            MESSAGE_MANAGE
        )
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        event.author.openPrivateChannel().queue { it.sendTyping().complete() }

        if (event.isFromType(TEXT)) {
            return if (botPermissions.contains(MESSAGE_MANAGE)) event.replyInDmAndLog("Registering is only allowed in DMs to protect your in-game ID. Please give it a go here!")
                .also { event.message.delete().queue() }
            else event.replyInDmAndLog("Registering is only allowed in DMs to protect your in-game ID. Please give it a go here and delete your previous message!")
        }

        val registrant = object {
            val discordId = event.author.id
            val discordTag = event.author.asTag
            val inGameId = parameters.getString(IN_GAME_ID)
            val inGameName = parameters.getString(IN_GAME_NAME)
        }

        transaction {
            val farmers = Farmer.all().toList()
            val backup = AuxBrain.getFarmerBackup(registrant.inGameId)

            // Check if the Discord user is already known, otherwise create a new user
            val discordUser: DiscordUser = DiscordUser.findById(registrant.discordId)
                ?: DiscordUser.new(registrant.discordId) {
                    this.discordTag = registrant.discordTag
                }

            // Check if this Discord user hasn't already registered that in-game name
            if (discordUser.farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName }) return@transaction event.replyAndLogWarning(
                "You are already registered with the in-game names: `${discordUser.farmers.joinToString("`, `") { it.inGameName }}`."
            ).also { rollback() }

            // Check if someone else hasn't already registered that in-game name
            if (farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName }) return@transaction event.replyAndLogWarning(
                "Someone else has already registered the in-game name `${registrant.inGameName}`."
            ).also { rollback() }

            // Check if any back-up was found with the in-game ID
            if (backup?.game == null || backup.stats == null) return@transaction event.replyAndLogWarning(
                """
                No account found with in-game ID `${registrant.inGameId}`. Did you enter your ID (not name!) correctly?
                Type `${event.client.textualPrefix}$name --help` for more info, hints and tips on how to use this command.
                """.trimIndent()
            ).also { rollback() }

            // Check if the in-game name matches with the in-game name belonging to the in-game ID's account
            if (!listOf(backup.userId, backup.eiUserId).contains(registrant.inGameId) || registrant.inGameName.toLowerCase() != backup.userName.toLowerCase()) return@transaction event.replyAndLogWarning(
                """
                The in-game name you entered (`${registrant.inGameName}`) does not match the name on record (`${backup.userName}`)
                If this is you, please register with `${event.client.textualPrefix}$name ${backup.userId} ${backup.userName}`
                """.trimIndent()
            ).also { rollback() }

            // Add the new in-game name
            val farmer = Farmer.new(discordUser, backup) ?: return@transaction event.replyAndLogWarning(
                "Failed to save the new registration to the database. Please contact the bot maintainer."
            )

            // Finally confirm the registration
            if (discordUser.farmers.filterNot { it.inGameId == registrant.inGameId }.none()) event.replyAndLogSuccess(
                "You have been registered with the in-game name `${farmer.inGameName}`, welcome!"
            ) else event.replyAndLogSuccess("You are now registered with the in-game name `${backup.userName}`, as well as `${
                discordUser.farmers
                    .filterNot { it.inGameId == registrant.inGameId }
                    .joinToString(" `, ` ") { it.inGameName }
            }`!")
        }
    }
}
