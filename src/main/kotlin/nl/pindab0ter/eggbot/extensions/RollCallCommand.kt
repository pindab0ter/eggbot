package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import dev.kord.common.entity.Permission.ManageChannels
import dev.kord.common.entity.Permission.ManageRoles
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.DEFAULT_ROLE_COLOR
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.contract
import nl.pindab0ter.eggbot.helpers.createChannel
import nl.pindab0ter.eggbot.helpers.createRole
import nl.pindab0ter.eggbot.helpers.multipartRespond
import nl.pindab0ter.eggbot.model.createRollCall
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.view.rollCallResponse
import org.jetbrains.exposed.sql.transactions.transaction

class RollCallCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    class RollCallArguments : Arguments() {
        val contract: Contract by contract()
        val basename: String by string {
            name = "name"
            description = "The base for the co-op names"
            validate {
                failIf("Co-op names cannot contain spaces") { value.contains(' ') }
            }
        }
        val createRoles: Boolean by createRole()
        val createChannels: Boolean by createChannel()
    }

    override suspend fun setup() = config.servers.forEach { server ->
        publicSlashCommand(::RollCallArguments) {
            name = "roll-call"
            description = "Create co-ops for a contract"
            locking = true
            guild(server.snowflake)

            requiredPerms += listOf(
                ManageRoles,
                ManageChannels,
            )

            check {
                hasRole(server.role.admin)
                passIf(event.interaction.user.id == config.botOwner)
            }

            action {
                transaction(databases[server.name]) {
                    val coops = createRollCall(arguments.basename, arguments.contract.maxCoopSize)
                        // First create all co-ops
                        .map { (name, farmers) ->
                            Coop.new {
                                this.contractId = arguments.contract.id
                                this.name = name
                            }.also { it.farmers = farmers }
                        }
                        // Then create roles and channels for all the successfully created co-ops
                        .onEach { coop ->
                            // TODO: Progress bar?

                            runBlocking {
                                // Create and assign roles
                                if (arguments.createRoles) {
                                    val role = guild?.createRole {
                                        name = coop.name
                                        mentionable = true
                                        color = DEFAULT_ROLE_COLOR
                                    }

                                    if (role != null) {
                                        coop.roleId = role.id
                                        coop.farmers.forEach { farmer ->
                                            guild?.getMemberOrNull(farmer.discordUser.snowflake)
                                                ?.addRole(role.id, "Roll call for ${arguments.contract.name}")
                                        }
                                    } else {
                                        this@RollCallCommand.logger.error { "Failed to create role for co-op ${coop.name}" }
                                    }
                                }

                                // Create and assign channel
                                if (arguments.createChannels) {
                                    val channel = guild?.createTextChannel(coop.name) {
                                        parentId = server.channel.coopsGroup
                                        reason = "Roll call for ${arguments.contract.name}"
                                    }
                                    coop.channelId = channel?.id
                                }
                            }
                        }

                    runBlocking {
                        guild?.rollCallResponse(arguments.contract, coops)?.let { multipartRespond(it) }
                    }
                }
            }
        }
    }
}
