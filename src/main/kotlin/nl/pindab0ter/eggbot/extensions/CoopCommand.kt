package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.Color
import dev.kord.common.entity.Permission.ManageChannels
import dev.kord.common.entity.Permission.ManageRoles
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.flow.firstOrNull
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.configuredGuild
import nl.pindab0ter.eggbot.helpers.contract
import nl.pindab0ter.eggbot.helpers.createChannel
import nl.pindab0ter.eggbot.helpers.createRole
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class CoopCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() {
        class AddCoopArguments : Arguments() {
            val contract: Contract by contract()
            val coopId: String by string {
                name = "coop"
                description = "The co-op ID. Can be found in #roll-call or in-game."

                validate {
                    failIf(value.contains(" "), "Co-op ID cannot contain spaces.")
                }
            }
            val createRole: Boolean by createRole()
            val createChannel: Boolean by createChannel()
            val preEmptive: Boolean by defaultingBoolean {
                name = "pre-emptive"
                description = "Add the co-op even if it doesn't exist (yet)."
                defaultValue = false
            }
        }

        ephemeralSlashCommand(::AddCoopArguments) {
            name = "add-coop"
            description = "Register a co-op so it shows up in the co-ops info listing."
            requiredPerms += listOf(
                ManageRoles,
                ManageChannels,
            )
            guild(Config.guild)

            action {
                val contract = arguments.contract
                val coopId = arguments.coopId.lowercase()

                // Check if co-op is already registered
                transaction {
                    Coop.find { (Coops.name eq coopId) and (Coops.contractId eq contract.id) }.firstOrNull()
                }?.let { coop ->
                    respond { content = "Co-op `${coop.name}` is already registered for _${contract.name}_." }
                    return@action
                }

                // Check if a role with the same name exists
                if (arguments.createRole) configuredGuild?.roles?.firstOrNull { role -> role.name == coopId }?.let { role ->
                    respond { content = "**Error:** The role ${role.mention} already exists." }
                    return@action
                }

                // Fetch the co-op status to see if it exists
                val coopStatus: CoopStatus? = AuxBrain.getCoopStatus(arguments.contract.id, coopId)

                // Finish if the co-op status couldn't be found, and it isn't being created pre-emptively
                if (coopStatus == null && !arguments.preEmptive) {
                    respond { content = "No co-op found for contract _${contract.name}_ with ID `${coopId}`" }
                    return@action
                }

                // Create the co-op
                val coop = transaction {
                    Coop.new {
                        name = coopId
                        contractId = contract.id
                    }
                }

                // Finish if no role needs to be created
                if (!arguments.createRole) {
                    respond {
                        content = buildString {
                            append("Registered co-op `${coop.name}` for contract _${contract.name}_")
                            if (arguments.preEmptive && coopStatus == null) append(", even though the co-op was not found")
                            append(".")
                        }
                    }
                    return@action
                }

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
                        append("the channel ${channel.mention}")
                    }
                    if (coopStatus == null && arguments.preEmptive) append(", even though the co-op was not found")
                    append(".")
                }

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

                respond {
                    content = responseBuilder.toString()
                }
            }
        }

        class RemoveCoopArguments : Arguments() {
            val coopId: String by string {
                name = "name"
                description = "The co-op ID. Can be found in #roll-call or in-game."

                validate {
                    failIf(value.contains(" "), "Co-op ID cannot contain spaces.")
                }

                autoComplete {
                    val coopInput: String = command.options["name"]?.value as String? ?: ""

                    val coops = transaction {
                        Coop
                            .find { Coops.name like "$coopInput%" }
                            .limit(25)
                            .orderBy(Coops.name to SortOrder.ASC)
                            .associate { coop -> Pair(coop.name, coop.name) }
                    }
                    suggestStringMap(coops)
                }
            }
        }

        ephemeralSlashCommand(::RemoveCoopArguments) {
            name = "remove-coop"
            description = "Remove a coop and it's corresponding role and/or channel (does not affect the co-op in-game)"
            requiredPerms += listOf(
                ManageRoles,
                ManageChannels,
            )
            guild(Config.guild)

            check {
                hasRole(Config.adminRole)
                passIf(event.interaction.user.id == Config.botOwner)
            }

            action {
                val coop: Coop? = transaction {
                    Coop.find { Coops.name eq arguments.coopId }.firstOrNull()
                }

                if (coop == null) {
                    respond { content = "Could not find that co-op" }
                    return@action
                }

                val role = coop.roleId?.let { guild?.getRoleOrNull(it) }
                val channel = coop.channelId?.let { guild?.getChannelOrNull(it) }

                try {
                    val roleName = role?.name
                    val channelName = channel?.name
                    role?.delete("Removed by ${user.asUser().username} using `/co-op remove`")
                    channel?.delete("Removed by ${user.asUser().username} using `/co-op remove`")
                    transaction { coop.delete() }
                    respond {
                        content = buildString {
                            append("Successfully deleted co-op")
                            if (roleName != null || channelName != null) {
                                append(" as well as")
                                if (roleName != null) {
                                    append(" role `@$roleName`")
                                    if (channelName != null) append(" and")
                                }
                                if (channelName != null) append(" channel ${channelName}")
                            }
                        }
                    }
                } catch (_: RestRequestException) {
                    respond { content = "Could not remove the co-op" }
                }
            }
        }
    }
}