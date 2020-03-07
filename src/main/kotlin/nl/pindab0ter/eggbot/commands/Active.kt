package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.jda.EggBotCommand
import org.jetbrains.exposed.sql.transactions.transaction

object Active : EggBotCommand() {

    init {
        category = FarmersCategory
        name = "active"
        help = "Set yourself as active, taking part in co-ops again."
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
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
