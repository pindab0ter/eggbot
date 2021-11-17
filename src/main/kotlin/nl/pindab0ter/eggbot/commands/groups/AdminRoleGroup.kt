package nl.pindab0ter.eggbot.commands.groups

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashGroup
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission.*
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
