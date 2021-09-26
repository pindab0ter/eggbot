package nl.pindab0ter.eggbot.commands

import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.commands.groups.channelGroup
import nl.pindab0ter.eggbot.commands.groups.coopGroup
import nl.pindab0ter.eggbot.commands.groups.roleGroup
import nl.pindab0ter.eggbot.commands.groups.rollCallGroup
import nl.pindab0ter.eggbot.model.Config

@KordPreview
val adminCommand: suspend SlashCommand<out Arguments>.() -> Unit = {
    val log = KotlinLogging.logger {}

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
