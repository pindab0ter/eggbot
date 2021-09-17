package nl.pindab0ter.eggbot.commands

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.EPHEMERAL
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission.ManageChannels
import dev.kord.common.entity.Permission.ManageRoles
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.Channel
import dev.kord.rest.json.JsonErrorCode
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.firstOrNull
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.configuredGuild
import nl.pindab0ter.eggbot.helpers.coopId
import nl.pindab0ter.eggbot.helpers.discard
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
val adminCommand: suspend SlashCommand<out Arguments>.() -> Unit = {
    val log = KotlinLogging.logger {}

    name = "admin"
    description = "All tools available to admins"

    guild(Config.guild)
    allowUser(Config.botOwner)
    allowRole(Config.adminRole)

    group("role") {
        description = "Add and remove roles"

        class AddRoleArguments : Arguments() {
            val member: Member by member(
                displayName = "member",
                description = "The member to assign the role to",
                requiredGuild = { Config.guild },
                useReply = true,
            )
        }

        class RemoveRoleArguments : Arguments() {
            val member: Member by member(
                displayName = "member",
                description = "The member to assign the role to",
                requiredGuild = { Config.guild },
                useReply = true,
            )
            val role: Role by role(
                displayName = "role",
                description = "The role to remove",
                requiredGuild = { Config.guild },
            )
        }

        class DeleteRoleArguments : Arguments() {
            val role: Role by role(
                displayName = "role",
                description = "The role to remove",
                requiredGuild = { Config.guild },
            )
        }

        subCommand(::AddRoleArguments) {
            name = "add"
            description = "Add a test role to someone"
            autoAck = EPHEMERAL
            requirePermissions(ManageRoles)

            action {
                guild?.createRole {
                    name = "Test Role"
                    hoist = true
                }?.let { role ->
                    arguments.member.addRole(role.id)
                    ephemeralFollowUp { content = "Successfully added ${role.mention} to ${arguments.member.mention}." }
                } ?: ephemeralFollowUp { content = "Failed to create role for ${arguments.member.mention}." }
            }
        }

        subCommand(::RemoveRoleArguments) {
            name = "remove"
            description = "Remove a specific role from someone"
            autoAck = EPHEMERAL
            requirePermissions(ManageRoles)

            action {
                arguments.member.removeRole(arguments.role.id)
                ephemeralFollowUp { content = "Successfully removed ${arguments.role.mention} to ${arguments.member.mention}" }
            }
        }

        subCommand(::DeleteRoleArguments) {
            name = "delete"
            description = "Delete a specific role"
            autoAck = EPHEMERAL
            requirePermissions(ManageRoles)

            action {
                val roleName = "`@${arguments.role.name}`"
                try {
                    arguments.role.delete()
                    ephemeralFollowUp { content = "Successfully deleted $roleName." }
                } catch (exception: KtorRequestException) {
                    if (exception.error?.code == JsonErrorCode.PermissionLack) ephemeralFollowUp {
                        content = "Failed to delete role ${arguments.role.mention}. The bot’s role must be higher than the role it’s trying to delete."
                    } else ephemeralFollowUp {
                        content = exception.error?.message ?: exception.localizedMessage
                    }
                }
            }
        } // Delete Role
    } // Role group

    group("channels") {
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
            autoAck = EPHEMERAL
            requirePermissions(ManageChannels)

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
        } // Create channel

        subCommand(::DeleteChannelArguments) {
            name = "delete"
            description = "Delete a channel"
            autoAck = EPHEMERAL
            requirePermissions(ManageChannels)

            action {
                val channelName = arguments.channel.mention
                arguments.channel.delete("Deleted by ${user.mention} through bot")
                ephemeralFollowUp {
                    content = "Succesfully deleted channel $channelName"
                }
            }
        } // Delete channel
    } // Channel group

    group("coop") {
        description = "Add and remove co-ops"

        class AddCoopArguments : Arguments() {
            val contract: Contract by contractChoice()
            val coopId: String by coopId()
            val createRole: Boolean by defaultingBoolean(
                displayName = "create-role",
                description = "Whether to create a role for this co-op. Defaults to ‘True’.",
                defaultValue = true,
            )
            val createChannel: Boolean by defaultingBoolean(
                displayName = "create-channel",
                description = "Whether to create a channel for this co-op. Defaults to ‘True’.",
                defaultValue = true,
            )
            val preEmptive: Boolean by defaultingBoolean(
                displayName = "pre-emptive",
                description = "Add the co-op even if it doesn’t exist (yet).",
                defaultValue = true
            )
        }

        subCommand(::AddCoopArguments) {
            name = "add"
            description = "Register a co-op so it shows up in the co-ops info listing."
            requiredPerms += listOf(
                ManageRoles,
                ManageChannels,
            )
            autoAck = PUBLIC

            action {
                val contract = arguments.contract
                val coopId = arguments.coopId.lowercase()

                // Check if roles can be created if required
                if (configuredGuild == null && arguments.createRole) return@action publicFollowUp {
                    content = "${Config.emojiWarning} Could not get server info. Please try without creating roles or contact the bot maintainer."
                }.discard()

                // Check if co-op is already registered
                if (transaction {
                        Coop.find {
                            (Coops.name eq coopId) and (Coops.contractId eq contract.id)
                        }.empty().not()
                    }) return@action publicFollowUp { content = "Co-op is already registered." }.discard()

                // Check if a role with the same name exists
                if (arguments.createRole) configuredGuild?.roles?.firstOrNull { role -> role.name == coopId }
                    ?.let { role ->
                        return@action publicFollowUp {
                            content = "${Config.emojiWarning} The role ${role.mention} already exists."
                        }.discard()
                    }

                // Fetch the co-op status to see if it exists
                val coopStatus: CoopStatus? = AuxBrain.getCoopStatus(arguments.contract.id, coopId)

                if (coopStatus == null && !arguments.preEmptive) return@action publicFollowUp {
                    // Finish if the co-op status couldn't be found;
                    // it most likely doesn't exist yet and this co-op is added in anticipation
                    content = "${Config.emojiWarning} No co-op found for contract _${contract.name}_ with ID `${coopId}`"
                }.discard()


                // Create the co-op
                val coop = transaction {
                    Coop.new {
                        name = coopId
                        contractId = contract.id
                    }
                }

                // Finish if no role needs to be created
                if (!arguments.createRole) return@action publicFollowUp {
                    content = "Registered co-op `${coop.name}` for contract _${contract.name}_."
                }.discard()

                // Create the role
                val role = guild?.createRole {
                    name = coopId
                    color = Color(15, 212, 57)
                    // TODO: Change back to false
                    hoist = true
                    mentionable = true
                }

                // TODO: Try/Catch PermissionLack
                val channel = if (arguments.createChannel) guild?.createTextChannel(coop.name) {
                    parentId = Config.coopsGroupChannel
                    name = coopId
                    topic = "_${coop.name}_ vs. _${contract.name}_"
                } else null

                transaction {
                    // TODO: Notify on fail
                    if (role != null) coop.roleId = role.id
                    if (channel != null) coop.channelId = channel.id
                }

                // TODO: Build a string saying whether the channel was created, the role was created an assigned
                // Finish if the role does not need assigning
                if (coopStatus == null && !arguments.preEmptive) return@action publicFollowUp {
                    content = "Registered co-op `${coop.id}` for contract _${contract.name}_"
                    // content = "Registered co-op `${coop.id}` for contract _${contract.name}_, with the role ${role.mention}"
                }.discard()

                val successes = mutableListOf<Member>()
                val failures = mutableListOf<String>()

                // Assign the role to each member
                coopStatus?.contributors?.map { contributor ->
                    val member = transaction {
                        Farmer.find { Farmers.id eq contributor.userId }.firstOrNull()
                    }?.discordId?.let { configuredGuild?.getMemberOrNull(it) }

                    if (member != null && role != null) {
                        member.addRole(role.id)
                        successes.add(member)
                    } else failures.add(contributor.userName)
                }

                publicFollowUp {
                    content = buildString {
                        appendLine("Registered co-op `${coopStatus?.coopId ?: arguments.coopId}` for contract _${contract.name}_.")
                        if (successes.isNotEmpty()) {
                            appendLine()
                            appendLine("The following players have been assigned the role ${role?.mention}:")
                            successes.forEach { member -> appendLine(member.mention) }
                        }
                        if (failures.isNotEmpty()) {
                            appendLine()
                            appendLine("Unable to assign the following players their role:")
                            appendLine("```")
                            failures.forEach { userName -> appendLine(userName) }
                            appendLine("```")
                        }
                    }
                }
            }
        } // Coops Add

        class RemoveCoopArguments : Arguments() {
            val coopId: String? by optionalString(
                displayName = "name",
                description = "The co-op ID. Can be found in #roll-call or in-game."
            )
            val channel: Channel? by optionalChannel(
                displayName = "channel",
                description = "The channel associated with the co-op to remove",
                requiredGuild = { Config.guild }
            )
            val role: Role? by optionalRole(
                displayName = "role",
                description = "The role associated with the co-op to remove",
                requiredGuild = { Config.guild }
            )
        }

        subCommand(::RemoveCoopArguments) {
            name = "remove"
            description = "Remove a coop"
            requiredPerms += listOf(
                ManageRoles,
                ManageChannels,
            )
            autoAck = PUBLIC

            action {
                if (listOf(
                        arguments.coopId != null,
                        arguments.channel != null,
                        arguments.role != null,
                ).count { it } > 2) return@action publicFollowUp {
                    content = "Please use only one method to choose a co-op to remove"
                }.discard()

                val coop: Coop = when {
                    arguments.coopId != null -> transaction {
                        Coop.find { Coops.name eq arguments.coopId!! }.firstOrNull()
                    }
                    arguments.channel != null -> transaction {
                        Coop.find { Coops.channelId eq arguments.channel!!.id.asString }.firstOrNull()
                    }
                    arguments.role != null -> transaction {
                        Coop.find { Coops.roleId eq arguments.role!!.id.asString }.firstOrNull()
                    }
                    else -> return@action publicFollowUp {
                        content = "You must choose a co-op to remove"
                    }.discard()
                } ?: return@action publicFollowUp {
                    content = "Could not find that co-op"
                }.discard()

                val role = coop.roleId?.let { guild?.getRoleOrNull(it) }
                val channel = coop.channelId?.let { guild?.getChannelOrNull(it) }

                role?.delete()
                channel?.delete()
                transaction { coop.delete() }

                publicFollowUp {
                    content = "Succesfully deleted co-op"
                }
            }
        }

    } // Coops Group

    group("roll-call") {
        description = "Manage roll calls"

        subCommand {
            name = "create"
            description = "Create teams for a contract"

            check {

            }

            action {
                // TODO: Create channels
            }
        }

        subCommand {
            name = "clear"
            description = "Remove all teams for a contract"

            check {

            }

            action {
                // TODO: Remove channels
            }
        }
    }
}
