package nl.pindab0ter.eggbot.commands.groups

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashGroup
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.types.respond

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission.*
import dev.kord.core.behavior.createTextChannel
import nl.pindab0ter.eggbot.helpers.discard
import nl.pindab0ter.eggbot.model.Config

@KordPreview
val adminChannelGroup: suspend SlashGroup.() -> Unit = {
    description = "Add and remove channels"

    class CreateChannelArguments : Arguments() {
        val channelName by string {
            name = "name"
            description = "The name for the channel"
        }
        val parentChannel by optionalChannel {
            name = "parent"
            description = "The parent channel"
            requiredGuild = { Config.guild }
        }
    }

    class DeleteChannelArguments : Arguments() {
        val channel by channel {
            name = "channel"
            description = "The channel to delete"
            requiredGuild = { Config.guild }
        }
    }

    ephemeralSubCommand(::CreateChannelArguments) {
        name = "create"
        description = "Create a channel"
        requireBotPermissions(ManageChannels)

        action {
            val channel = guild?.createTextChannel(arguments.channelName) {
                parentId = arguments.parentChannel?.id
                reason = "Created by bot"
            } ?: return@action respond {
                content = "Failed to create channel ${arguments.channelName}"
            }.discard()

            respond {
                content = "Created channel ${channel.mention}"
            }
        }
    }

    ephemeralSubCommand(::DeleteChannelArguments) {
        name = "delete"
        description = "Delete a channel"
        requireBotPermissions(ManageChannels)

        action {
            val channelName = arguments.channel.data.name
            arguments.channel.delete("Deleted by ${user.mention} through bot")
            respond {
                content = "Succesfully deleted channel $channelName"
            }
        }
    }
}
