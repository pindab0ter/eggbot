package nl.pindab0ter.eggbot.commands.groups

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.model.Config

@KordPreview
val adminCommandGroup: suspend SlashCommand<PublicSlashCommandContext<Arguments>, out Arguments>.() -> Unit = {

    name = "admin"
    description = "All tools available to admins"

    guild(Config.guild)
    allowUser(Config.botOwner)
    allowRole(Config.adminRole)

    group("role", adminRoleGroup)

    group("channel", adminChannelGroup)

    group("co-op", adminCoopGroup)

    group("roll-call", adminRollCallGroup)
}
