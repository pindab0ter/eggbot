package nl.pindab0ter.eggbot.commands.groups

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashGroup
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permission.*
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.Channel
import kotlinx.coroutines.flow.firstOrNull
import nl.pindab0ter.eggbot.commands.contract
import nl.pindab0ter.eggbot.helpers.configuredGuild
import nl.pindab0ter.eggbot.helpers.coopId
import nl.pindab0ter.eggbot.helpers.createRole
import nl.pindab0ter.eggbot.helpers.createChannel
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
val coopGroup: suspend SlashGroup.() -> Unit = {
    description = "Add and remove co-ops"

    class AddCoopArguments : Arguments() {
        val contract: Contract by contract()
        val coopId: String by coopId()
        val createRole: Boolean by createRole()
        val createChannel: Boolean by createChannel()
        val preEmptive: Boolean by defaultingBoolean(
            displayName = "pre-emptive",
            description = "Add the co-op even if it doesn't exist (yet).",
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
                mentionable = true
            }

            val channel = if (arguments.createChannel) guild?.createTextChannel(coop.name) {
                parentId = Config.coopsGroupChannel
                name = coopId
                topic = "_${coop.name}_ vs. _${contract.name}_"
            } else null

            transaction {
                if (role != null) coop.roleId = role.id
                if (channel != null) coop.channelId = channel.id
            }

            val responseBuilder = StringBuilder().apply {
                append("Registered co-op `${coop.name}` for contract _${contract.name}_")

                if (role != null) append(", with the role ${role.mention}")
                if (channel != null) {
                    if (role != null) append(" and ") else append(", ")
                    append("the channel ${channel.mention}.")
                } else append(".")
            }

            // Finish if the role does not need assigning
            if (coopStatus == null && !arguments.preEmptive) return@action publicFollowUp {
                content = responseBuilder.toString()
            }.discard()

            val successes = mutableListOf<String>()
            val failures = mutableListOf<String>()

            // Assign the role to each member
            coopStatus?.contributors?.map { contributor ->
                val member = transaction {
                    Farmer.find { Farmers.id eq contributor.userId }.firstOrNull()
                }?.discordId?.let { configuredGuild?.getMemberOrNull(it) }

                if (member != null && role != null) {
                    member.addRole(role.id)
                    successes.add(member.mention)
                } else failures.add(contributor.userName)
            }

            responseBuilder.apply {
                if (successes.isNotEmpty()) {
                    appendLine()
                    appendLine("The following players have been assigned the role ${role?.mention}:")
                    successes.forEach { mention -> appendLine(mention) }
                }
                if (failures.isNotEmpty()) {
                    appendLine()
                    appendLine("Unable to assign the following players their role:")
                    appendLine("```")
                    failures.forEach { userName -> appendLine(userName) }
                    appendLine("```")
                }
            }

            publicFollowUp {
                content = responseBuilder.toString()
            }
        }
    }

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
        name = "delete"
        description = "Delete a coop and it's corresponding role and/or channel"
        requiredPerms += listOf(
            ManageRoles,
            ManageChannels,
        )
        autoAck = PUBLIC

        action {
            if (listOf(
                    arguments.coopId != null,
                    arguments.channel != null,
                    arguments.role != null
                ).count { it } > 2
            ) return@action publicFollowUp {
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
                content = "Successfully deleted co-op"
            }
        }
    }
}
