package nl.pindab0ter.eggbot.commands.groups

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashGroup
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission.ManageChannels
import dev.kord.common.entity.Permission.ManageRoles
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.converters.contract
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.createRollCall
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.view.rollCallResponse
import org.jetbrains.exposed.sql.transactions.transaction


private val DEFAULT_ROLE_COLOR = Color(15, 212, 57) // #0FD439

@KordPreview
val rollCallGroup: suspend SlashGroup.() -> Unit = {
    description = "Manage roll calls"

    class CreateRollCallArguments : Arguments() {
        val contract: Contract by contract()
        val basename: String by string(
            displayName = "name",
            description = "The base for the team names",
            validator = { _, value ->
                !value.contains(Regex("\\s"))
            }
        )
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
                content = "${Config.emojiWarning} Could not get server info. Please try without creating roles or channels or else please contact the bot maintainer."
            }.discard()

            val coops = transaction {
                createRollCall(arguments.basename, arguments.contract.maxCoopSize)
                    // First create all co-ops
                    .map { (name, farmers) ->
                        Coop.new {
                            this.contractId = arguments.contract.id
                            this.name = name
                            this.leader = farmers
                                .filter { farmer -> farmer.canBeCoopLeader }
                                .maxByOrNull { farmer -> farmer.earningsBonus }
                            this.farmers = farmers
                        }
                    }
                    // Then create roles and channels for all the successfully created co-ops
                    .onEach { coop ->
                        runBlocking {

                            // TODO: Progress bar?

                            // Create and assign roles
                            if (arguments.createRoles) configuredGuild?.createRole {
                                name = coop.name
                                mentionable = true
                                color = DEFAULT_ROLE_COLOR
                            }?.let { role ->
                                coop.roleId = role.id
                                coop.farmers.forEach { farmer ->
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
                Coop.find { Coops.contractId eq arguments.contract.id }
            }

            if (coops.empty()) return@action respond {
                content = "No co-ops found for _${arguments.contract.name}_."
            }.discard()

            // TODO: Remove channels
            // TODO: Delete all roles
            // TODO: Delete all channels
            // TODO: Delete all database entries
            // TODO: Response
        }
    }
}