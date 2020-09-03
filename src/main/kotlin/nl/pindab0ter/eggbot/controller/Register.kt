package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.api.entities.ChannelType.TEXT
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.helpers.prophecyEggResearchLevel
import nl.pindab0ter.eggbot.helpers.soulEggResearchLevel
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object Register : EggBotCommand() {

    private val log = KotlinLogging.logger { }
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
                    "You in-game ID. Can be found by going to the Menu → Settings → More and looking in the" +
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
            if (botPermissions.contains(MESSAGE_MANAGE)) "Registering is only allowed in DMs to protect your in-game ID. Please give it a go here!".let {
                event.message.delete().queue()
                event.replyInDm(it)
                log.debug { it }
            } else "Registering is only allowed in DMs to protect your in-game ID. Please give it a go here and delete your previous message!".let {
                event.replyInDm(it)
                log.debug { it }
            }
            return
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
            if (discordUser.farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName })
                "You are already registered with the in-game names: `${discordUser.farmers.joinToString("`, `") { it.inGameName }}`.".let {
                    event.replyWarning(it)
                    log.debug { it }
                    rollback()
                    return@transaction
                }

            // Check if someone else hasn't already registered that in-game name
            if (farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName })
                "Someone else has already registered the in-game name `${registrant.inGameName}`.".let {
                    event.replyWarning(it)
                    log.debug { it }
                    rollback()
                    return@transaction
                }

            // Check if any back-up was found with the in-game ID
            if (backup?.game == null || backup.stats == null)
                // TODO: Check if name has spaces in it, if it does, surround the quoted name with spaces
                ("No account found with in-game ID `${registrant.inGameId}`. Did you enter your ID (not name!) correctly?\n" +
                        "To register, type `${event.client.textualPrefix}$name $arguments` without the brackets.").let {
                    event.replyError(it)
                    log.debug { it }
                    rollback()
                    return@transaction
                }

            // Check if the in-game name matches with the in-game name belonging to the in-game ID's account
            if (registrant.inGameId != backup.userId || registrant.inGameName.toLowerCase() != backup.userName.toLowerCase())
                ("The in-game name you entered (`${registrant.inGameName}`) does not match the name on record (`${backup.userName}`)\n" +
                        "If this is you, please register with `${event.client.textualPrefix}$name ${backup.userId} ${backup.userName}`").let {
                    event.replyError(it)
                    log.debug { it }
                    rollback()
                    return@transaction
                }

            // Add the new in-game name
            Farmer.new(registrant.inGameId) {
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

            // Finally confirm the registration
            if (discordUser.farmers.filterNot { it.inGameId == registrant.inGameId }.none())
                "You have been registered with the in-game name `${backup.userName}`, welcome!".let {
                    event.replySuccess(it)
                    log.debug { it }
                }
            else "You are now registered with the in-game name `${backup.userName}`, as well as `${discordUser.farmers
                .filterNot { it.inGameId == registrant.inGameId }
                .joinToString(" `, ` ") { it.inGameName }
            }`!".let {
                event.replySuccess(it)
                log.debug { it }
            }
        }
    }
}
