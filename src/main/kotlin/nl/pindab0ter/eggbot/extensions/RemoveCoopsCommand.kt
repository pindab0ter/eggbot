package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission.*
import dev.kord.rest.request.RestRequestException
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.extensions.RemoveCoopsCommand.DeletionStatus.Type.CHANNEL
import nl.pindab0ter.eggbot.extensions.RemoveCoopsCommand.DeletionStatus.Type.ROLE
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

    data class DeletionStatus(
        val type: Type,
        val deleted: Boolean,
    ) {
        enum class Type {
            CHANNEL, ROLE
        }
    }

    class RemoveCoopsArguments : Arguments() {
        val contract: Contract by contract()
    }

    override suspend fun setup() = config.servers.forEach { server ->
        publicSlashCommand(::RemoveCoopsArguments) {
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
                val coops = transaction(databases[server.name]) {
                    Coop.find { Coops.contractId eq arguments.contract.id }.toList()
                }

                if (coops.isEmpty()) {
                    respond { content = "No co-ops found for _${arguments.contract.name}_." }
                    return@action
                }

                val statuses = coops
                    .map<Coop, Pair<Coop, MutableSet<DeletionStatus>>> { coop: Coop -> coop to mutableSetOf() }
                    .map { (coop: Coop, statuses: MutableSet<DeletionStatus>) ->
                        val coopName = coop.name

                        try {
                            guild?.getChannelOrNull(coop.channelId)?.delete("Roll Call for ${arguments.contract.name} cleared by ${user.asUser().username}")
                            statuses.add(DeletionStatus(CHANNEL, true))
                        } catch (exception: RestRequestException) {
                            statuses.add(DeletionStatus(CHANNEL, false))
                            this@RemoveCoopsCommand.logger.warn { "Failed to delete channel for co-op $coopName: ${exception.localizedMessage}" }
                        }

                        try {
                            guild?.getRoleOrNull(coop.roleId)?.delete("Roll Call for ${arguments.contract.name} cleared by ${user.asUser().username}")
                            statuses.add(DeletionStatus(ROLE, true))
                        } catch (exception: RestRequestException) {
                            statuses.add(DeletionStatus(ROLE, false))
                            this@RemoveCoopsCommand.logger.warn { "Failed to delete role for co-op $coopName: ${exception.localizedMessage}" }
                        }

                        transaction(databases[server.name]) { coop.delete() }

                        coopName to statuses.toSet()
                    }

                respond {
                    content = removeCoopsResponse(arguments.contract, statuses)
                }
            }
        }
    }
}