package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission.ManageChannels
import dev.kord.common.entity.Permission.ManageRoles
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.DEFAULT_ROLE_COLOR
import nl.pindab0ter.eggbot.extensions.RollCallExtension.DeletionStatus.Type.CHANNEL
import nl.pindab0ter.eggbot.extensions.RollCallExtension.DeletionStatus.Type.ROLE
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.createRollCall
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.view.rollCallResponse
import org.jetbrains.exposed.sql.transactions.transaction

class RollCallExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "AdminExtension"

    private data class DeletionStatus(
        val type: Type,
        val deleted: Boolean,
    ) {
        enum class Type {
            CHANNEL, ROLE
        }
    }

    override suspend fun setup() {
        publicSlashCommand {

            name = "roll-call"
            description = "Manage roll calls"

            guild(Config.guild)
            allowUser(Config.botOwner)
            allowRole(Config.adminRole)

            class CreateRollCallArguments : Arguments() {
                val contract: Contract by contract()
                val basename: String by string {
                    name = "name"
                    description = "The base for the team names"
                    validate {
                        if (value.contains(' ')) fail("Team names cannot contain spaces")
                    }
                }
                val createRoles: Boolean by createRole()
                val createChannels: Boolean by createChannel()
            }

            class ClearRollCallArguments : Arguments() {
                val contract: Contract by contract()
            }

            publicSubCommand(::CreateRollCallArguments) {
                name = "create"
                description = "Create teams for a contract"
                requiredPerms += listOf(
                    ManageRoles,
                    ManageChannels,
                )

                action {
                    // Check if roles or channels can be created if required
                    if (configuredGuild == null && (arguments.createRoles || arguments.createChannels)) return@action respond {
                        content = "${Config.emojiWarning} Could not get server info. " +
                                "Please try without creating roles or channels or else please contact the bot maintainer."
                    }.discard()

                    val coops = transaction {
                        createRollCall(arguments.basename, arguments.contract.maxCoopSize)
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
                                    if (arguments.createRoles) configuredGuild?.createRole {
                                        name = coop.name
                                        mentionable = true
                                        color = DEFAULT_ROLE_COLOR
                                    }?.let { role ->
                                        coop.roleId = role.id
                                        coop.farmers.map { farmer ->
                                            configuredGuild
                                                ?.getMemberOrNull(farmer.discordUser.snowflake)
                                                ?.addRole(role.id, "Roll call for ${arguments.contract.name}")
                                        }
                                    }

                                    // Create and assign channel
                                    if (arguments.createChannels) configuredGuild?.createTextChannel(coop.name) {
                                        parentId = Config.coopsGroupChannel
                                        reason = "Roll call for ${arguments.contract.name}"
                                    }?.let { channel -> coop.channelId = channel.id }
                                }
                            }
                    }

                    multipartRespond(rollCallResponse(arguments.contract, coops))
                }
            }

            publicSubCommand(::ClearRollCallArguments) {
                name = "clear"
                description = "Remove all teams for a contract"
                requiredPerms += listOf(
                    ManageRoles,
                    ManageChannels,
                )

                action {
                    val coops = transaction {
                        Coop.find { Coops.contractId eq arguments.contract.id }.toList()
                    }

                    if (coops.isEmpty()) return@action respond {
                        content = "No co-ops found for _${arguments.contract.name}_."
                    }.discard()


                    val statuses = coops
                        .map<Coop, Pair<Coop, MutableSet<DeletionStatus>>> { coop: Coop -> coop to mutableSetOf() }
                        .map { (coop: Coop, statuses: MutableSet<DeletionStatus>) ->
                            val coopName = coop.name

                            try {
                                coop.channel?.delete("Roll Call for ${arguments.contract.name} cleared by ${user.asUser().username}")
                                statuses.add(DeletionStatus(CHANNEL, true))
                            } catch (exception: RestRequestException) {
                                statuses.add(DeletionStatus(CHANNEL, false))
                                this@publicSubCommand.logger.warn { "Failed to delete channel for co-op $coopName: ${exception.localizedMessage}" }
                            }

                            try {
                                coop.role?.delete("Roll Call for ${arguments.contract.name} cleared by ${user.asUser().username}")
                                statuses.add(DeletionStatus(ROLE, true))
                            } catch (exception: RestRequestException) {
                                statuses.add(DeletionStatus(ROLE, false))
                                this@publicSubCommand.logger.warn { "Failed to delete role for co-op $coopName: ${exception.localizedMessage}" }
                            }

                            transaction { coop.delete() }

                            coopName to statuses.toSet()
                        }

                    respond {
                        content = buildString {
                            val successfullyDeletedChannels = statuses.count { (_, statuses: Set<DeletionStatus>) ->
                                statuses.any { deletionStatus: DeletionStatus ->
                                    deletionStatus.type == CHANNEL && deletionStatus.deleted
                                }
                            }
                            val successfullyDeletedRoles = statuses.count { (_, statuses: Set<DeletionStatus>) ->
                                statuses.any { deletionStatus: DeletionStatus ->
                                    deletionStatus.type == ROLE && deletionStatus.deleted
                                }
                            }

                            appendLine("Cleared the roll-call for __${arguments.contract.name}__:")
                            appendLine("Successfully deleted $successfullyDeletedChannels channels and $successfullyDeletedRoles roles.")

                            statuses
                                .map { (coopName, statuses) ->
                                    coopName to statuses
                                        .filterNot(DeletionStatus::deleted)
                                        .map { deletionStatus -> deletionStatus.type }
                                        .sorted()
                                }
                                .filter { (_, statuses) -> statuses.isNotEmpty() }
                                .sortedWith(compareBy { it.first })
                                .let { failedToDelete ->
                                    if (failedToDelete.isNotEmpty()) appendLine("Failed to delete:")
                                    failedToDelete.forEach { (coopName, types) ->
                                        append("For `$coopName`: ")
                                        when (types.size) {
                                            1 -> append(types.first().name.lowercase())
                                            else -> types.joinToString(" and ") { type -> type.name.lowercase() }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}