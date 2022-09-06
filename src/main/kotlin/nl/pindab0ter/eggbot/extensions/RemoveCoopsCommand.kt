package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission.*
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.extensions.RemoveCoopsCommand.CleanupStatus.Status.*
import nl.pindab0ter.eggbot.helpers.contract
import nl.pindab0ter.eggbot.helpers.getChannelOrNull
import nl.pindab0ter.eggbot.helpers.getRoleOrNull
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.view.removeCoopsResponse
import org.jetbrains.exposed.sql.transactions.transaction

class RemoveCoopsCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    data class CleanupStatus(
        val role: Status,
        val channel: Status,
    ) {
        fun has(status: Status): Boolean = role == status || channel == status

        enum class Status {
            NO_ACTION, DELETED, NOT_FOUND, FAILED
        }
    }

    class RemoveCoopsArguments : Arguments() {
        val contract: Contract by contract()
    }

    override suspend fun setup() = config.servers.forEach { server ->
        ephemeralSlashCommand(::RemoveCoopsArguments) {
            name = "remove-coops"
            description = "Remove all co-ops for a contract"
            locking = true
            guild(server.snowflake)
            requireBotPermissions(
                ManageChannels,
                ManageRoles,
                MentionEveryone
            )

            check {
                hasRole(server.role.admin)
                passIf(event.interaction.user.id == config.botOwner)
            }

            action {
                val coops: List<Coop> = transaction(databases[server.name]) {
                    Coop.find { Coops.contractId eq arguments.contract.id }.toList()
                }

                if (coops.isEmpty()) {
                    respond { content = "No co-ops found for __${arguments.contract.name}__." }
                    return@action
                }

                val statuses = coops.associate { coop: Coop ->
                    val status = CleanupStatus(
                        role = try {
                            when {
                                coop.roleId == null -> NO_ACTION
                                guild?.getRoleOrNull(coop.roleId) == null -> NOT_FOUND
                                else -> {
                                    guild?.getRoleOrNull(coop.roleId)
                                        ?.delete("Roll Call for ${arguments.contract.name} cleared by ${user.asUser().username}")
                                    DELETED
                                }
                            }
                        } catch (exception: Exception) {
                            logger.error { "Failed to delete role for co-op ${coop.name}: ${exception.localizedMessage}" }
                            FAILED
                        },
                        channel = try {
                            when {
                                coop.channelId == null -> NO_ACTION
                                guild?.getChannelOrNull(coop.channelId) == null -> NOT_FOUND
                                else -> {
                                    guild?.getChannelOrNull(coop.channelId)
                                        ?.delete("Roll Call for ${arguments.contract.name} cleared by ${user.asUser().username}")
                                    DELETED
                                }
                            }
                        } catch (exception: Exception) {
                            logger.error { "Failed to delete channel for co-op ${coop.name}: ${exception.localizedMessage}" }
                            FAILED
                        }
                    )

                    val name = coop.name
                    transaction(databases[server.name]) { coop.delete() }
                    name to status
                }

                respond {
                    content = removeCoopsResponse(arguments.contract, statuses)
                }
            }
        }
    }
}