package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import net.dv8tion.jda.api.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.helpers.prophecyEggResearchLevel
import nl.pindab0ter.eggbot.helpers.soulEggResearchLevel
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object Migrate : EggBotCommand() {

    private const val EGG_INC_ID = "egg inc id"

    init {
        category = FarmersCategory
        name = "migrate"
        help = "Migrate your account to register with the new Egg, Inc. ID. **DM only!"
        hidden = true
        registrationRequired = true
        parameters = listOf(
            UnflaggedOption(EGG_INC_ID)
                .setRequired(REQUIRED)
                .setHelp(
                    "Your Egg, Inc. ID. Can be found by going to the Menu → Settings → More and looking in the" +
                            "bottom of that screen. The Egg, Inc. ID starts with \"EI\" followed by 16 digits."
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

        if (event.isFromType(ChannelType.TEXT)) {
            return if (Register.botPermissions.contains(MESSAGE_MANAGE)) event.replyInDmAndLog("Registering is only allowed in DMs to protect your in-game ID. Please give it a go here!")
                .also { event.message.delete().queue() }
            else event.replyInDmAndLog("Registering is only allowed in DMs to protect your in-game ID. Please give it a go here and delete your previous message!")
        }

        val eggIncId = parameters.getString(EGG_INC_ID).toUpperCase()

        transaction {
            val discordUser: DiscordUser = DiscordUser.findById(event.author.id)
                ?: return@transaction event.replyAndLogWarning("You are not registered.")

            if (discordUser.farmers.all { farmer -> farmer.inGameId.startsWith("EI") }) return@transaction event.replyAndLogSuccess(
                "Your in-game ID has already been updated."
            )

            val backup = AuxBrain.getFarmerBackup(eggIncId)

            if (backup?.game == null || backup.stats == null) return@transaction event.replyAndLogWarning(
                """
            No account found with in-game ID `$eggIncId`. Did you enter your new Egg, Inc. ID correctly?
            Type `${event.client.textualPrefix}${Register.name} --help` for more info, hints and tips on how to use this command.
            """.trimIndent()
            )

            val farmer = discordUser.farmers.find { farmer ->
                farmer.inGameName == backup.userName
            } ?: return@transaction event.replyAndLogWarning(
                """
                The account for the given Egg, Inc. ID (${backup.userName}) does not match any of the accounts you are registered with (${discordUser.farmers.joinToString { it.inGameName }}).
                If you think this is an error, please contact the bot maintainer.
                """.trimIndent())

            farmer.delete()

            Farmer.new(backup.eiUserId) {
                this.discordUser = discordUser
                this.inGameName = backup.userName.replace('`', '\'')
                this.soulEggsLong = backup.game.soulEggsLong
                this.soulEggsDouble = backup.game.soulEggsDouble
                this.prophecyEggs = backup.game.prophecyEggs
                this.soulEggResearchLevel = backup.game.soulEggResearchLevel
                this.prophecyEggResearchLevel = backup.game.prophecyEggResearchLevel
                this.prestiges = backup.stats.prestigeCount
                this.droneTakedowns = backup.stats.droneTakedowns
                this.eliteDroneTakedowns = backup.stats.droneTakedownsElite
                this.lastUpdated = DateTime.now()
            }

            event.replyAndLogSuccess(
                "You have successfully migrated your account!"
            )
        }
    }
}
