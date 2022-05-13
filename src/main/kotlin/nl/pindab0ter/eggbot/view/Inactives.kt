package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.helpers.formatCompact
import nl.pindab0ter.eggbot.helpers.formatDays
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.joda.time.DateTime.now
import org.joda.time.Duration


fun inactivesResponse(inactiveDiscordUsers: List<DiscordUser>): String = buildString {
    if (inactiveDiscordUsers.isEmpty()) {
        appendLine("No users have set themselves as inactive.")
        return@buildString
    }

    if (inactiveDiscordUsers.size == 1) appendLine("One user has set themselves as inactive: ```")
    else appendLine("${inactiveDiscordUsers.size} users have set themselves as inactive: ```")

    inactiveDiscordUsers.forEach { inactiveDiscordUser ->
        append("${inactiveDiscordUser.mention} ")
        append("${inactiveDiscordUser.inactiveUntil!!.formatCompact()} ")
        append("(${Duration(now(), inactiveDiscordUser.inactiveUntil!!).formatDays()})")
        appendLine()
    }

    appendLine("```")
}
