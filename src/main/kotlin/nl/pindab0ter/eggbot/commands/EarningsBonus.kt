package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.commands.categories.UsersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction

object EarningsBonus : Command() {

    private val log = KotlinLogging.logger { }

    // TODO: Add arguments

    init {
        name = "earnings-bonus"
        aliases = arrayOf("eb", "earningsbonus", "earning-bonus", "earningbonus")
        help = "Shows your EB, EB rank and how much EB till your next rank"
        category = UsersCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent): Unit = transaction {

        // TODO: If not called with arguments, get up-to-date EB for all Discord user's Farmers
        // TODO: If called with arguments, calculate scenario

        log.trace { "Getting Earnings Bonus for ${event.author.name}…" }

        event.author.openPrivateChannel().queue { it.sendTyping().queue() }

        DiscordUser.findById(event.author.id)?.farmers?.forEach { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId) { (backup, _) ->
                if (backup == null) return@getFarmerBackup
                farmer.update(backup)
                event.replyInDm("${farmer.inGameName}:\nCurrent EB: ${farmer.earningsBonus}") {
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
            }
        }
    }
}

