package nl.pindab0ter.eggbot.kord.commands

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.createRole
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import kotlinx.coroutines.flow.firstOrNull
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.kord.converters.contractChoice
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config.emojiWarning
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InActive.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress
import nl.pindab0ter.eggbot.view.coopFinishedIfBankedResponse
import nl.pindab0ter.eggbot.view.coopFinishedResponse
import nl.pindab0ter.eggbot.view.coopInfoResponse
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import nl.pindab0ter.eggbot.model.database.Farmer.Companion as DatabaseFarmer
import nl.pindab0ter.eggbot.model.simulation.Farmer as SimulationFarmer

class CoopArguments : Arguments()

class CoopInfoArguments : Arguments() {
    val contract: Contract by contractChoice()
    val coopId: String by coopId()
    val compact: Boolean by compact()
}

class CoopAddArguments : Arguments() {
    val contract: Contract by contractChoice()
    val coopId: String by coopId()
    val createRole: Boolean by boolean(
        displayName = "create-role",
        description = "Whether to create a role for this co-op."
    )
    val force: Boolean by boolean(
        displayName = "force",
        description = "Add the co-op even if it doesn't exist (yet)."
    )
}

@KordPreview
val coopCommand: suspend SlashCommand<out CoopArguments>.() -> Unit = {
    name = "coop"
    description = "Perform actions to do with single co-ops."

    subCommand(::CoopInfoArguments) {
        name = "info"
        description = "See the current status and player contribution of a specific co-op."
        autoAck = PUBLIC

        action {
            val contract = arguments.contract
            val coopStatus = AuxBrain.getCoopStatus(arguments.contract.id, arguments.coopId)
                ?: return@action publicFollowUp {
                    content = "No co-op found for contract __${contract.name}__ with ID `${arguments.coopId}`"
                }.discard()
            val compact = arguments.compact

            when (val status = CoopContractStatus(contract, coopStatus, arguments.coopId)) {
                is Abandoned -> publicFollowUp {
                    content = """
                        `${status.coopStatus.coopId}` vs. __${contract.name}__:
                            
                        This co-op has no members.""".trimIndent()
                }

                is Failed -> publicFollowUp {
                    content = """
                        `${status.coopStatus.coopId}` vs. __${contract.name}__:
                            
                        This co-op has not reached their final goal.""".trimIndent()
                }

                is Finished -> publicMultipartFollowUp(coopFinishedResponse(coopStatus, contract, compact))

                is InProgress -> {
                    val sortedState = status.state.copy(
                        farmers = status.state.farmers.sortedByDescending(SimulationFarmer::currentEggsLaid)
                    )

                    publicMultipartFollowUp(when (status) {
                        is InProgress.FinishedIfBanked -> coopFinishedIfBankedResponse(sortedState, compact)
                        else -> coopInfoResponse(sortedState, compact)
                    })
                }
            }
        }
    }

    subCommand(::CoopAddArguments) {
        name = "add"
        description = "Register a co-op so it shows up in the co-ops info listing."
        autoAck = PUBLIC

        action {
            val contract = arguments.contract
            val coopId = arguments.coopId.lowercase()

            // Check if roles can be created if required
            if (configuredGuild == null && arguments.createRole) return@action ephemeralFollowUp {
                content = "$emojiWarning Could not get server info. Please try without creating roles or contact the bot maintainer."
            }.discard()

            // Check if co-op is already registered
            if (transaction {
                    Coop.find {
                        (Coops.name eq coopId) and (Coops.contractId eq contract.id)
                    }.empty().not()
                }) return@action ephemeralFollowUp { content = "Co-op is already registered." }.discard()

            // Check if a role with the same name exists
            if (arguments.createRole) configuredGuild?.roles?.firstOrNull { role -> role.name == coopId }?.let { role ->
                return@action ephemeralFollowUp {
                    content = "$emojiWarning The role ${role.mention} already exists."
                }.discard()
            }

            // Fetch the co-op status to see if it exists
            val coopStatus: CoopStatus? = AuxBrain.getCoopStatus(arguments.contract.id, coopId)
            if (coopStatus == null && !arguments.force) return@action ephemeralFollowUp {
                content = "$emojiWarning No co-op found for contract __${contract.name}__ with ID `${coopId}`"
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
                content = "Registered co-op `${coop.id}` for contract `${coop.contractId}`."
            }.discard()

            // Create the role
            val role: Role = configuredGuild!!.createRole {
                name = coopId
                color = Color(15, 212, 57)
                hoist = false
                mentionable = true
            }

            // Finish if the role does not need assigning
            if (coopStatus == null) return@action publicFollowUp {
                content = "Registered co-op `${coop.id}` for contract `${coop.contractId}`, with the role ${role.mention}"
            }.discard()

            val successes = mutableListOf<Member>()
            val failures = mutableListOf<String>()

            // Assign the role to each member
            coopStatus.contributors.map { contributor ->
                transaction {
                    DatabaseFarmer.find { Farmers.id eq contributor.userId }.firstOrNull()
                }?.let { farmer ->
                    configuredGuild?.getMemberOrNull(farmer.discordId)?.let { member ->
                        member.addRole(role.id)
                        successes.add(member)
                    } ?: failures.add(farmer.inGameName)
                } ?: failures.add(contributor.userName)
            }

            publicFollowUp {
                content = buildString {
                    appendLine("Registered co-op `${coopStatus.coopId}` for contract `${coopStatus.contractId}`.")
                    if (successes.isNotEmpty()) {
                        appendLine()
                        appendLine("The following players have been assigned the role ${role.mention}:")
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
    }
}
