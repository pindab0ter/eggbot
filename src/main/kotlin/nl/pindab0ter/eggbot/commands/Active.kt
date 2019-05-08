package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction

object Active : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "active"
        help = "Set yourself as active, taking part in co-ops again."
        // category = ContractsCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        val discordUser = transaction { DiscordUser.findById(event.author.id) }

        if (discordUser == null) "You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

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
