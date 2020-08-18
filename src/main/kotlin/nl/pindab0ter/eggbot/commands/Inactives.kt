package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.utilities.appendPaddingCharacters
import nl.pindab0ter.eggbot.utilities.asCompact
import nl.pindab0ter.eggbot.utilities.asDays
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

        when {
            inactiveDiscordUsers.isEmpty() -> {
                event.reply("There are no users that have set themselves as inactive.")
                return
            }
            else -> {
                event.reply(message(inactiveDiscordUsers))
                return
            }
        }
    }

    fun message(inactiveDiscordUsers: List<DiscordUser>): String = StringBuilder().apply {
        val longestName = inactiveDiscordUsers.maxByOrNull { it.discordName.length }!!.discordName
        val now = DateTime.now()

        if (inactiveDiscordUsers.size == 1) appendLine("One user has set themselves as inactive: ```")
        else appendLine("${inactiveDiscordUsers.size} users have set themselves as inactive: ```")

        inactiveDiscordUsers.forEach { inactiveDiscordUser ->
            append("${inactiveDiscordUser.discordName} ")
            appendPaddingCharacters(inactiveDiscordUser.discordName, longestName)
            append("${inactiveDiscordUser.inactiveUntil!!.asCompact()} ")
            append("(${Duration(now, inactiveDiscordUser.inactiveUntil!!).asDays()})")
            appendLine()
        }
        appendLine("```")
    }.toString()
}
