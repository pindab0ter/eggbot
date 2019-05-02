package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction

object EarningsBonus : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "earnings-bonus"
        aliases = arrayOf("eb", "earningsbonus", "earning-bonus", "earningbonus")
        help = "Shows your EB, EB rank and how much EB till your next rank"
        arguments = "[compact]"
        // category = UsersCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        // TODO: If called with arguments, calculate scenario

        val farmers = transaction { DiscordUser.findById(event.author.id)?.farmers?.toList() }

        @Suppress("FoldInitializerAndIfToElvis")
        if (farmers.isNullOrEmpty()) "You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        farmers.forEach { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId) { (backup, _) ->
                if (backup == null) "Could not get information on ${farmer.inGameName}".let {
                    event.replyWarning(it)
                    log.warn { it }
                    return@getFarmerBackup
                }

                transaction { farmer.update(backup) }

                Messages.earningsBonus(farmer, event.arguments.isNotEmpty()).let {
                    if (event.channel.id == Config.botCommandsChannel) {
                        event.reply(it)
                    } else event.replyInDm(it) {
                        if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                    }
                }
            }
        }
    }
}
