package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction

object Active : EggBotCommand() {

    init {
        category = FarmersCategory
        name = "active"
        help = "Set yourself as active, taking part in co-ops again."
        sendTyping = false
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(
        event: CommandEvent,
        parameters: JSAPResult,
    ) = transaction {
        val discordUser = DiscordUser.findById(event.author.id)!!

        if (discordUser.isActive) event.reply("You are already active.")
        else {
            discordUser.inactiveUntil = null
            event.replySuccess("You are now active again.")
        }
    }
}
