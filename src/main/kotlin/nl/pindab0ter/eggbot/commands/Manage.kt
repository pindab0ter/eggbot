package nl.pindab0ter.eggbot.commands

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.EPHEMERAL
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.createRole
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import kotlinx.coroutines.flow.firstOrNull
import nl.pindab0ter.eggbot.helpers.configuredGuild
import nl.pindab0ter.eggbot.helpers.coopId
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
val manageCommand: suspend SlashCommand<out Arguments>.() -> Unit = {

    group("coop") {
        class AddCoopArguments : Arguments() {
            val contract: Contract by contractChoice()
            val coopId: String by coopId()
            val createRole: Boolean by boolean(
                displayName = "create-role",
                description = "Whether to create a role for this co-op."
            )
            val force: Boolean by defaultingBoolean(
                displayName = "force",
                description = "Add the co-op even if it doesn't exist (yet).",
                defaultValue = false,
            )
        }

        subCommand(::AddCoopArguments) {
            name = "add"
            description = "Register a co-op so it shows up in the co-ops info listing."
            requiredPerms += listOf(
                Permission.ManageRoles,
                Permission.ManageChannels,
            )

            action {
                val contract = arguments.contract
                val coopId = arguments.coopId.lowercase()

                // Check if roles can be created if required
                if (configuredGuild == null && arguments.createRole) return@action ephemeralFollowUp {
                    content = "${Config.emojiWarning} Could not get server info. Please try without creating roles or contact the bot maintainer."
                }.discard()

                // Check if co-op is already registered
                if (transaction {
                        Coop.find {
                            (Coops.name eq coopId) and (Coops.contractId eq contract.id)
                        }.empty().not()
                    }) return@action ephemeralFollowUp { content = "Co-op is already registered." }.discard()

                // Check if a role with the same name exists
                if (arguments.createRole) configuredGuild?.roles?.firstOrNull { role -> role.name == coopId }
                    ?.let { role ->
                        return@action ephemeralFollowUp {
                            content = "${Config.emojiWarning} The role ${role.mention} already exists."
                        }.discard()
                    }

                // Fetch the co-op status to see if it exists
                val coopStatus: CoopStatus? = AuxBrain.getCoopStatus(arguments.contract.id, coopId)
                if (coopStatus == null && !arguments.force) return@action ephemeralFollowUp {
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
                if (!arguments.createRole) return@action ephemeralFollowUp {
                    content = "Registered co-op `${coop.name}` for contract _${contract.name}_."
                }.discard()

                // Create the role
                val role: Role = configuredGuild!!.createRole {
                    name = coopId
                    color = Color(15, 212, 57)
                    hoist = false
                    mentionable = true
                }

                // Finish if the role does not need assigning
                if (coopStatus == null) return@action ephemeralFollowUp {
                    content = "Registered co-op `${coop.id}` for contract _${contract.name}_, with the role ${role.mention}"
                }.discard()

                val successes = mutableListOf<Member>()
                val failures = mutableListOf<String>()

                // Assign the role to each member
                coopStatus.contributors.map { contributor ->
                    transaction {
                        Farmer.find { Farmers.id eq contributor.userId }.firstOrNull()
                    }?.let { farmer ->
                        configuredGuild?.getMemberOrNull(farmer.discordId)?.let { member ->
                            member.addRole(role.id)
                            successes.add(member)
                        } ?: failures.add(farmer.inGameName)
                    } ?: failures.add(contributor.userName)
                }

                ephemeralFollowUp {
                    content = buildString {
                        appendLine("Registered co-op `${coopStatus.coopId}` for contract _${contract.name}_.")
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
        } // Coops Add
    } // Coops Group

    group("roll-call") {
        subCommand {
            name = "create"
            description = ""

            check {

            }

            action {
                // TODO: Create channels
            }
        }

        subCommand {
            name = "clear"
            description = ""

            check {

            }

            action {
                // TODO: Remove channels
            }
        }
    }
}
