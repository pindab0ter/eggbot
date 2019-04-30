package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.ChannelType.TEXT
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object Register : Command() {
    private val log = KotlinLogging.logger { }

    init {
        name = "register"
        arguments = "<in-game id> <in-game name>"
        help = "Register on this server with your in-game name and in-game ID. **DM only!**"
        // category = UsersCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.author.openPrivateChannel().queue { it.sendTyping().queue() }

        if (event.isFromType(TEXT)) {
            if (botPermissions.contains(MESSAGE_MANAGE)) "Registering is only allowed in DMs to protect your in-game ID. Please give it a go here!".let {
                event.message.delete().queue()
                event.replyInDm(it)
                log.trace { it }
            } else "Registering is only allowed in DMs to protect your in-game ID. Please give it a go here and delete your previous message!".let {
                event.replyInDm(it)
                log.trace { it }
            }
            return
        }

        if (event.arguments.size < 2) missingArguments.let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        val registrant = object {
            val discordId = event.author.id
            val discordTag = event.author.asTag
            val inGameId = event.arguments.first()
            val inGameName = event.arguments.tail().joinToString(" ")
        }

        transaction {
            val farmers = Farmer.all().toList()
            val (backup, _) = AuxBrain.getFarmerBackup(registrant.inGameId.toUpperCase())

            // Check if the Discord user is already known, otherwise create a new user
            val discordUser: DiscordUser = DiscordUser.findById(registrant.discordId)
                ?: DiscordUser.new(registrant.discordId) {
                    this.discordTag = registrant.discordTag
                }

            // Check if this Discord user hasn't already registered that in-game name
            if (discordUser.farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName })
                "You are already registered with the in-game names: `${discordUser.farmers.joinToString("`, `") { it.inGameName }}`.".let {
                    event.replyWarning(it)
                    log.trace { it }
                    return@transaction
                }

            // Check if someone else hasn't already registered that in-game name
            if (farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName })
                "Someone else has already registered the in-game name `${registrant.inGameName}`.".let {
                    event.replyWarning(it)
                    log.trace { it }
                    return@transaction
                }

            // Check if any back-up was found with the in-game ID
            if (backup == null || !backup.hasData())
                ("No account found with in-game ID `${registrant.inGameId}`. Did you enter your ID (not name!) correctly?\n" +
                        "To register, type `${event.client.textualPrefix}$name $arguments` without the brackets.").let {
                    event.replyError(it)
                    log.trace { it }
                    return@transaction
                }

            // Check if the in-game name matches with the in-game name belonging to the in-game ID's account
            if (registrant.inGameId != backup.userid || registrant.inGameName.toLowerCase() != backup.name.toLowerCase())
                ("The in-game name you entered (`${registrant.inGameName}`) does not match the name on record (`${backup.name}`)\n" +
                        "If this is you, please register with `${event.client.textualPrefix}$name ${backup.userid} ${backup.name}`").let {
                    event.replyError(it)
                    log.trace { it }
                    return@transaction
                }

            // Add the new in-game name
            Farmer.new(registrant.inGameId) {
                this.discordUser = discordUser
                this.inGameName = backup.name.replace('`', '\'')
                this.soulEggs = backup.data.soulEggs
                this.prophecyEggs = backup.data.prophecyEggs
                this.soulBonus = backup.data.soulBonus
                this.prophecyBonus = backup.data.prophecyBonus
                this.prestiges = backup.stats.prestigeCount
                this.droneTakedowns = backup.stats.droneTakedowns
                this.eliteDroneTakedowns = backup.stats.droneTakedownsElite
                this.lastUpdated = DateTime.now()
            }

            // Finally confirm the registration
            if (discordUser.farmers.filterNot { it.inGameId == registrant.inGameId }.none())
                "You have been registered with the in-game name `${backup.name}`, welcome!".let {
                    event.replySuccess(it)
                    log.trace { it }
                }
            else "You are now registered with the in-game name `${backup.name}`, as well as `${discordUser.farmers
                .filterNot { it.inGameId == registrant.inGameId }
                .joinToString(" `, ` ") { it.inGameName }
            }`!".let {
                event.replySuccess(it)
                log.trace { it }
            }
        }
    }
}
