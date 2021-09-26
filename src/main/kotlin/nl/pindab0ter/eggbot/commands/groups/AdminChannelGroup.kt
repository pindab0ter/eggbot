package nl.pindab0ter.eggbot.commands.groups

import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashGroup
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.createTextChannel
import nl.pindab0ter.eggbot.helpers.discard
import nl.pindab0ter.eggbot.model.Config

@KordPreview
val channelGroup: suspend SlashGroup.() -> Unit = {
    description = "Add and remove channels"

    class CreateChannelArguments : Arguments() {
        val channelName by string(
            displayName = "name",
            description = "The name for the channel",
        )
        val parentChannel by optionalChannel(
            displayName = "parent",
            description = "The parent channel",
            requiredGuild = { Config.guild },
        )
    }

    class DeleteChannelArguments : Arguments() {
        val channel by channel(
            displayName = "channel",
            description = "The channel to delete",
            requiredGuild = { Config.guild },
        )
    }

    subCommand(::CreateChannelArguments) {
        name = "create"
        description = "Create a channel"
        autoAck = AutoAckType.EPHEMERAL
        requirePermissions(Permission.ManageChannels)

        action {
            val channel = guild?.createTextChannel(arguments.channelName) {
                parentId = arguments.parentChannel?.id
                reason = "Created by bot"
            } ?: return@action ephemeralFollowUp {
                content = "Failed to create channel ${arguments.channelName}"
            }.discard()

            ephemeralFollowUp {
                content = "Created channel ${channel.mention}"
            }
        }
    }

    subCommand(::DeleteChannelArguments) {
        name = "delete"
        description = "Delete a channel"
        autoAck = AutoAckType.EPHEMERAL
        requirePermissions(Permission.ManageChannels)

        action {
            val channelName = arguments.channel.data.name
            arguments.channel.delete("Deleted by ${user.mention} through bot")
            ephemeralFollowUp {
                content = "Succesfully deleted channel $channelName"
            }
        }
    }
}
