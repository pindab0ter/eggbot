package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.utilities.PrerequisitesCheckResult
import nl.pindab0ter.eggbot.utilities.checkPrerequisites
import org.jetbrains.exposed.sql.transactions.transaction

object Active : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "active"
        help = "Set yourself as active, taking part in co-ops again."
        category = FarmersCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val discordUser = transaction { DiscordUser.findById(event.author.id)!! }

        when {
            discordUser.isActive -> {
                event.reply("You are already active.")
                return
            }
            else -> {
                transaction { discordUser.inactiveUntil = null }
                event.replySuccess("You are now active again.")
                return
            }
        }
    }
}
