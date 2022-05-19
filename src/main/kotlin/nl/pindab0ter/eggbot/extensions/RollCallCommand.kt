package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission.*
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.channel.Category
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
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.rollCallResponse
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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
            requireBotPermissions(
                ViewChannel,
                SendMessages,
                ManageChannels,
                ManageRoles,
                MentionEveryone,
            )

            check {
                hasRole(server.role.admin)
                passIf(event.interaction.user.id == config.botOwner)
                throwIfFailedWithMessage()

                val channel = guildFor(event)?.getChannelOrNull(server.channel.coopsGroup)
                failIf("Cannot create channels because the configured channel is not a \"Category\". Please contact the bot maintainer") {
                    channel !is Category
                }
                throwIfFailedWithMessage()

                failIfNot("Missing required permissions to set up channels. Please contact the bot maintainer.") {
                    channel?.botHasPermissions(
                        ViewChannel,
                        ManageChannels,
                        SendMessages,
                        MentionEveryone,
                    ) == true
                }
            }

            action {
                if (transaction { Farmer.count() == 0L }) {
                    respond { content = "No farmers registered." }
                    return@action
                }

                newSuspendedTransaction(null, databases[server.name]) {
                    val coops = createRollCall(
                        baseName = arguments.basename,
                        maxCoopSize = arguments.contract.maxCoopSize,
                        database = databases[server.name]
                    ).map { (name, farmers) ->
                        Coop.new {
                            this.contractId = arguments.contract.id
                            this.name = name
                        }.also { coop ->
                            coop.farmers = SizedCollection(farmers)
                        }
                    }

                    commit()

                    if (arguments.createChannels) {
                        coops.forEach createChannels@{ coop ->
                            val channel = guild?.createTextChannel(coop.name) {
                                parentId = server.channel.coopsGroup
                                reason = "Roll call for ${arguments.contract.name}"
                            }
                            coop.channelId = channel?.id
                        }
                        commit()
                    }

                    if (arguments.createRoles) {
                        coops.forEach createRoles@{ coop ->
                            val role = guild?.createRole {
                                name = coop.name
                                mentionable = true
                                color = DEFAULT_ROLE_COLOR
                            } ?: return@createRoles

                            coop.roleId = role.id
                            coop.farmers.forEach { farmer ->
                                guild?.getMemberOrNull(farmer.discordUser.snowflake)
                                    ?.addRole(role.id, "Roll call for ${arguments.contract.name}")
                            }
                        }
                        commit()
                    }

                    guild?.let { multipartRespond(it.rollCallResponse(arguments.contract, coops)) }
                        ?: respond { content = "**Error:** Could not get guild." }
                }
            }
        }
    }
}
