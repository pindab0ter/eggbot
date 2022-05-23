package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission.*
import dev.kord.core.behavior.channel.createTextChannel
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.Category
import mu.KotlinLogging
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.Plurality.PLURAL
import nl.pindab0ter.eggbot.model.createRollCall
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.DiscordUsers
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import nl.pindab0ter.eggbot.view.rollCallResponse
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.jodatime.CurrentDateTime
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

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
        val maxCoopSize: Int? by optionalInt {
            name = "max-coop-size"
            description = "How many farmers should be in each co-op"
        }
        val createRolesAndChannels: Boolean by createRolesAndChannels(PLURAL)
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

                val coopsCategoryChannel = guildFor(event)?.getChannelOfOrNull<Category>(server.channel.coopsGroup)
                failIf("Cannot create channels because the configured channel is not a \"Category\". Please contact the bot maintainer") {
                    coopsCategoryChannel !is Category
                }
                throwIfFailedWithMessage()

                failIfNot("Missing required permissions to set up channels. Please contact the bot maintainer.") {
                    coopsCategoryChannel?.botHasPermissions(
                        ViewChannel,
                        ManageChannels,
                        SendMessages,
                        MentionEveryone,
                    ) == true
                }
            }

            action {
                if (arguments.maxCoopSize != null && arguments.maxCoopSize!! > arguments.contract.maxCoopSize) {
                    respond { content = "The contract has a maximum co-op size of ${arguments.contract.maxCoopSize}" }
                    return@action
                }

                val startGetFarmers = System.currentTimeMillis()
                val farmers = transaction {
                    val activeFarmersQuery = Farmers.innerJoin(DiscordUsers)
                        .select { DiscordUsers.inactiveUntil.isNull() or (DiscordUsers.inactiveUntil less CurrentDateTime) }
                    Farmer.wrapRows(activeFarmersQuery).toList()
                }
                logger.debug { "Get farmers: ${System.currentTimeMillis() - startGetFarmers}ms" }

                if (farmers.isEmpty()) {
                    respond { content = "**Error:** Could not create a roll call because there are no active farmers." }
                    return@action
                }

                val maxCoopSize: Int = arguments.maxCoopSize
                    ?: if (arguments.contract.maxCoopSize <= 10) arguments.contract.maxCoopSize
                    else (arguments.contract.maxCoopSize * COOP_FILL_PERCENTAGE).roundToInt()

                newSuspendedTransaction(null, databases[server.name]) {
                    val startCreateCoops = System.currentTimeMillis()
                    val coops = createRollCall(arguments.basename, maxCoopSize, farmers)
                        .map { (name, farmers) ->
                            Coop.new {
                                this.contractId = arguments.contract.id
                                this.name = name
                            }.also { coop -> coop.farmers = SizedCollection(farmers) }
                        }
                        .sortedBy(Coop::name)

                    logger.debug { "Create co-ops: ${System.currentTimeMillis() - startCreateCoops}ms" }
                    commit()

                    if (arguments.createRolesAndChannels) {
                        val coopsCategoryChannel = guildFor(event)?.getChannelOfOrNull<Category>(server.channel.coopsGroup)

                        val startRolesAndChannels = System.currentTimeMillis()

                        // No need for async since we'll be rate-limited by Discord
                        coops.forEach createRolesAndChannels@{ coop ->
                            val startRoleAndChannel = System.currentTimeMillis()
                            // TODO: "Creating and assigning roles and channel to users for ${coop.name}..."
                            // Create and assign roles
                            val role = guild?.createRole {
                                name = coop.name
                                mentionable = true
                                color = DEFAULT_ROLE_COLOR
                                reason = "Roll call ${user.asUser().username} through ${this@publicSlashCommand.kord.getSelf().username} for \"${arguments.contract.name}\""
                            }

                            if (role != null) {
                                coop.roleId = role.id
                                coop.farmers.forEachAsync { farmer ->
                                    guild
                                        ?.getMemberOrNull(farmer.discordUser.snowflake)
                                        ?.addRole(role.id, "Roll call for ${arguments.contract.name}")
                                }
                            }

                            // Create channel
                            val channel = coopsCategoryChannel?.createTextChannel(coop.name) {
                                topic = "_${coop.name}_ vs. _${arguments.contract.name}_"
                                reason = "Roll call ${user.asUser().username} through ${this@publicSlashCommand.kord.getSelf().username} for \"${arguments.contract.name}\""
                            }
                            coop.channelId = channel?.id

                            // Send message in co-op channel
                            channel?.createMessage(buildString {
                                // Header
                                appendLine("**__Co-op ${role?.mention ?: coop.name} (`${coop.name}`)__**")

                                // Body
                                coop.farmers
                                    .sortedBy { farmer -> (farmer.inGameName ?: "") }
                                    .forEach { farmer ->
                                        if (farmer.isActive.not()) append("_")
                                        append(guild?.mentionUser(farmer.discordUser.snowflake))
                                        append(" (`${farmer.inGameName ?: NO_ALIAS}`)")
                                        if (farmer.isActive.not()) append(" (Inactive)_")
                                        appendLine()
                                    }
                                appendLine()
                            })
                            logger.debug { "${coop.name} done in ${System.currentTimeMillis() - startRoleAndChannel}ms" }
                            commit()
                        }

                        logger.debug { "Roles and channels: ${System.currentTimeMillis() - startRolesAndChannels}ms" }
                    }

                    multipartRespond(guild?.rollCallResponse(arguments.contract, coops) ?: emptyList())
                }
            }
        }
    }
}
