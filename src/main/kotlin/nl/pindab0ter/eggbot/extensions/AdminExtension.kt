package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission.ManageChannels
import dev.kord.common.entity.Permission.ManageRoles
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.rest.json.JsonErrorCode
import dev.kord.rest.request.KtorRequestException
import nl.pindab0ter.eggbot.helpers.discard
import nl.pindab0ter.eggbot.model.Config

class AdminExtension : Extension() {
    override val name: String = javaClass.simpleName

    override suspend fun setup() {
        publicSlashCommand {

            name = "admin"
            description = "All tools available to admins"

            guild(Config.guild)
            allowUser(Config.botOwner)
            allowRole(Config.adminRole)

            group("role") {
                description = "Add and remove roles"

                class AddRoleArguments : Arguments() {
                    val member: Member by member {
                        name = "member"
                        description = "The member to assign the role to"
                        requiredGuild = { Config.guild }
                        useReply = true
                    }
                }

                class RemoveRoleArguments : Arguments() {
                    val member: Member by member {
                        name = "member"
                        description = "The member to assign the role to"
                        requiredGuild = { Config.guild }
                        useReply = true
                    }
                    val role: Role by role {
                        name = "role"
                        description = "The role to remove"
                        requiredGuild = { Config.guild }
                    }
                }

                class DeleteRoleArguments : Arguments() {
                    val role: Role by role {
                        name = "role"
                        description = "The role to remove"
                        requiredGuild = { Config.guild }
                    }
                }

                ephemeralSubCommand(::AddRoleArguments) {
                    name = "add"
                    description = "Add a test role to someone"
                    requireBotPermissions(ManageRoles)

                    action {
                        guild?.createRole {
                            name = "Test Role"
                            hoist = true
                        }?.let { role ->
                            arguments.member.addRole(role.id)
                            respond { content = "Successfully added ${role.mention} to ${arguments.member.mention}." }
                        } ?: respond { content = "Failed to create role for ${arguments.member.mention}." }
                    }
                }

                ephemeralSubCommand(::RemoveRoleArguments) {
                    name = "remove"
                    description = "Remove a specific role from someone"
                    requireBotPermissions(ManageRoles)

                    action {
                        arguments.member.removeRole(arguments.role.id)
                        respond { content = "Successfully removed ${arguments.role.mention} to ${arguments.member.mention}" }
                    }
                }

                ephemeralSubCommand(::DeleteRoleArguments) {
                    name = "delete"
                    description = "Delete a specific role"
                    requireBotPermissions(ManageRoles)

                    action {
                        val roleName = "`@${arguments.role.name}`"
                        try {
                            arguments.role.delete()
                            respond { content = "Successfully deleted $roleName." }
                        } catch (exception: KtorRequestException) {
                            if (exception.error?.code == JsonErrorCode.PermissionLack) respond {
                                content = "Failed to delete role ${arguments.role.mention}. The bot’s role must be higher than the role it’s trying to delete."
                            } else respond {
                                content = exception.error?.message ?: exception.localizedMessage
                            }
                        }
                    }
                }
            }

            group("channel") {
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
        }
    }
}