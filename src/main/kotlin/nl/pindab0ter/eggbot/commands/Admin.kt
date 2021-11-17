package nl.pindab0ter.eggbot.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.commands.groups.channelGroup
import nl.pindab0ter.eggbot.commands.groups.coopGroup
import nl.pindab0ter.eggbot.commands.groups.roleGroup
import nl.pindab0ter.eggbot.commands.groups.rollCallGroup
import nl.pindab0ter.eggbot.model.Config

@KordPreview
val adminCommand: suspend SlashCommand<EphemeralSlashCommandContext<Arguments>, out Arguments>.() -> Unit = {

    name = "admin"
    description = "All tools available to admins"

    guild(Config.guild)
    allowUser(Config.botOwner)
    allowRole(Config.adminRole)

    group("role", roleGroup)

    group("channel", channelGroup)

    group("co-op", coopGroup)

    group("roll-call", rollCallGroup)
}
