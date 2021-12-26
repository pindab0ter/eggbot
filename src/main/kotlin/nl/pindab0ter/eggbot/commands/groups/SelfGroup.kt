package nl.pindab0ter.eggbot.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.commands.groups.selfActivityGroup
import nl.pindab0ter.eggbot.model.Config

@KordPreview
val selfGroup: suspend SlashCommand<EphemeralSlashCommandContext<Arguments>, out Arguments>.() -> Unit = {

    name = "self"
    description = "Manage your own account and status"

    guild(Config.guild)

    // group("status", selfStatusGroup)

    group("activity", selfActivityGroup)
}
