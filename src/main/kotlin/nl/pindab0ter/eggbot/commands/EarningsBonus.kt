package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.tooManyArguments
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.text.DecimalFormat

object EarningsBonus : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "earnings-bonus"
        aliases = arrayOf("eb", "earningsbonus", "earning-bonus", "earningbonus")
        help = "Shows your EB, EB rank and how much SE till your next rank"
        arguments = "[compact]"
        category = FarmersCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        val farmers = transaction {
            DiscordUser.findById(event.author.id)?.farmers?.toList()?.sortedBy { it.inGameName }
        }

        @Suppress("FoldInitializerAndIfToElvis")
        if (farmers.isNullOrEmpty()) "You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        if (event.arguments.size > 1) tooManyArguments.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val compact = event.arguments.any { it.startsWith("c") }

        farmers.forEach { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId) { (backup, _) ->
                if (backup == null) "Could not get information on ${farmer.inGameName}".let {
                    event.replyWarning(it)
                    log.warn { it }
                    return@getFarmerBackup
                }

                transaction { farmer.update(backup) }

                Messages.earningsBonus(farmer, compact).let {
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
