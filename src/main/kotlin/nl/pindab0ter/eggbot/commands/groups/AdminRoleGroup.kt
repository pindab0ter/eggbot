package nl.pindab0ter.eggbot.commands.groups

import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashGroup
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.createRole
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.rest.json.JsonErrorCode
import dev.kord.rest.request.KtorRequestException
import nl.pindab0ter.eggbot.model.Config

@KordPreview
val roleGroup: suspend SlashGroup.() -> Unit = {
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
        autoAck = AutoAckType.EPHEMERAL
        requirePermissions(Permission.ManageRoles)

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
        autoAck = AutoAckType.EPHEMERAL
        requirePermissions(Permission.ManageRoles)

        action {
            arguments.member.removeRole(arguments.role.id)
            ephemeralFollowUp { content = "Successfully removed ${arguments.role.mention} to ${arguments.member.mention}" }
        }
    }

    subCommand(::DeleteRoleArguments) {
        name = "delete"
        description = "Delete a specific role"
        autoAck = AutoAckType.EPHEMERAL
        requirePermissions(Permission.ManageRoles)

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
    }
}
