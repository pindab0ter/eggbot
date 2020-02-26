package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Duration

object Inactives : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "inactives"
        help = "Display a list of who have set themselves as inactive and until when."
        category = FarmersCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

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

    fun message(inactiveDiscordUsers: List<DiscordUser>): String =
        StringBuilder().apply {
            val longestName = inactiveDiscordUsers.maxBy { it.discordName.length }!!.discordName
            val now = DateTime.now()

            if (inactiveDiscordUsers.size == 1) appendln("One user has set themselves as inactive: ```")
            else appendln("${inactiveDiscordUsers.size} users have set themselves as inactive: ```")

            inactiveDiscordUsers.forEach { inactiveDiscordUser ->
                append("${inactiveDiscordUser.discordName} ")
                appendPaddingCharacters(inactiveDiscordUser.discordName, longestName)
                append("${inactiveDiscordUser.inactiveUntil!!.asCompact()} ")
                append("(${Duration(now, inactiveDiscordUser.inactiveUntil!!).asDays()})")
                appendln()
            }
            appendln("```")
        }.toString()
}
