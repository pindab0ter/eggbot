package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.Color
import dev.kord.common.entity.Permission.ManageChannels
import dev.kord.common.entity.Permission.ManageRoles
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import kotlinx.coroutines.flow.firstOrNull
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.contract
import nl.pindab0ter.eggbot.helpers.createChannel
import nl.pindab0ter.eggbot.helpers.createRole
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class AddCoopCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

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

    override suspend fun setup() = config.servers.forEach { server ->
        ephemeralSlashCommand(::AddCoopArguments) {
            name = "add-coop"
            description = "Register a co-op so it shows up in the co-ops info listing."
            guild(server.snowflake)

            action {
                if (arguments.createRole && guild?.botHasPermissions(ManageRoles)?.not() == true) {
                    respond { content = "**Error:** No permission to create channels" }
                    return@action
                }

                if (arguments.createChannel && guild?.botHasPermissions(ManageChannels)?.not() == true) {
                    respond { content = "**Error:** No permission to create channels" }
                    return@action
                }

                val contract = arguments.contract
                val coopId = arguments.coopId.lowercase()

                // Check if co-op is already registered
                transaction(databases[server.name]) {
                    Coop.find { (Coops.name eq coopId) and (Coops.contractId eq contract.id) }.firstOrNull()
                }?.let { coop ->
                    respond { content = "Co-op `${coop.name}` is already registered for _${contract.name}_." }
                    return@action
                }

                // Check if a role with the same name exists
                if (arguments.createRole) this@action.guild?.roles?.firstOrNull { role -> role.name == coopId }?.let { role ->
                    respond { content = "**Error:** The role ${role.mention} already exists." }
                    return@action
                }

                // Fetch the co-op status to see if it exists
                val coopStatus: CoopStatus? = arguments.contract.id.let { AuxBrain.getCoopStatus(it, coopId) }

                // Finish if the co-op status couldn't be found, and it isn't being created pre-emptively
                if (coopStatus == null && !arguments.preEmptive) {
                    respond { content = "No co-op found for contract _${contract.name}_ with ID `${coopId}`" }
                    return@action
                }

                // Create the co-op
                val coop = transaction(databases[server.name]) {
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
                val role = this@action.guild?.createRole {
                    name = coopId
                    color = Color(15, 212, 57)
                    mentionable = true
                }

                val channel = if (arguments.createChannel) this@action.guild?.createTextChannel(coop.name) {
                    parentId = server.channel.coopsGroup
                    name = coopId
                    topic = "_${coop.name}_ vs. _${contract.name}_"
                } else null

                transaction(databases[server.name]) {
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
                    val member = transaction(databases[server.name]) {
                        Farmer.find { Farmers.id eq contributor.userId }.firstOrNull()
                    }?.discordId?.let { this@action.guild?.getMemberOrNull(it) }

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
    }
}