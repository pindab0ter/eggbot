package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.helpers.appendPaddingCharacters
import nl.pindab0ter.eggbot.helpers.formatCompact
import nl.pindab0ter.eggbot.helpers.formatDays
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Duration

object Inactives : EggBotCommand() {

    init {
        category = FarmersCategory
        name = "inactives"
        help = "Display a list of who have set themselves as inactive and until when."
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val inactiveDiscordUsers = transaction {
            DiscordUser
                .find { DiscordUsers.inactiveUntil.isNotNull() and (DiscordUsers.inactiveUntil greater DateTime.now()) }
                .sortedBy { it.inactiveUntil }
                .toList()
        }

        if (inactiveDiscordUsers.isEmpty()) event.reply("There are no users that have set themselves as inactive.")
        else event.reply(message(inactiveDiscordUsers))
    }

    fun message(inactiveDiscordUsers: List<DiscordUser>): String = buildString {
        val longestName = inactiveDiscordUsers.maxByOrNull { it.discordName.length }!!.discordName
        val now = DateTime.now()

        if (inactiveDiscordUsers.size == 1) appendLine("One user has set themselves as inactive: ```")
        else appendLine("${inactiveDiscordUsers.size} users have set themselves as inactive: ```")

        inactiveDiscordUsers.forEach { inactiveDiscordUser ->
            append("${inactiveDiscordUser.discordName} ")
            appendPaddingCharacters(inactiveDiscordUser.discordName, longestName)
            append("${inactiveDiscordUser.inactiveUntil!!.formatCompact()} ")
            append("(${Duration(now, inactiveDiscordUser.inactiveUntil!!).formatDays()})")
            appendLine()
        }
        appendLine("```")
    }
}
