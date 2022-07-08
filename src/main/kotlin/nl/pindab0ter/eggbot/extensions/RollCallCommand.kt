package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission.*
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.createTextChannel
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.Category
import mu.KotlinLogging
import nl.pindab0ter.eggbot.COOP_FILL_PERCENTAGE
import nl.pindab0ter.eggbot.DEFAULT_ROLE_COLOR
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.AttemptStatus.COMPLETED
import nl.pindab0ter.eggbot.helpers.Plurality.PLURAL
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.createRollCall
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.DiscordUsers
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import nl.pindab0ter.eggbot.model.withProgressBar
import nl.pindab0ter.eggbot.view.coopChannelMessage
import nl.pindab0ter.eggbot.view.rollCallResponse
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.jodatime.CurrentDateTime
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.joda.time.DateTime.now
import kotlin.math.roundToInt

class RollCallCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    class RollCallArguments : Arguments() {
        val contract: Contract by coopContract()
        val basename: String by string {
            name = "name"
            description = "The base for the co-op names"
            validate {
                failIf("Co-op names cannot contain spaces") { value.contains(' ') }
            }
            mutate(String::lowercase)
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

                newSuspendedTransaction(null, databases[server.name]) {
                    val farmers = withProgressBar(
                        goal = Farmers.innerJoin(DiscordUsers)
                            .select { DiscordUsers.inactiveUntil.isNull() or (DiscordUsers.inactiveUntil less CurrentDateTime) }
                            .count().toInt(),
                        statusText = "Roll call for __${arguments.contract.name}__:\nChecking who has attempted this contract in the past…",
                        unit = "farmers",
                    ) {
                        Farmer.wrapRows(Farmers.innerJoin(DiscordUsers)
                            .select { DiscordUsers.inactiveUntil.isNull() or (DiscordUsers.inactiveUntil less CurrentDateTime) })
                            .associateWithAsync { AuxBrain.getFarmerBackup(it.eggIncId, databases[server.name]).also { increment() } }
                            .filterValues { backup -> backup != null && backup.attemptStatusFor(arguments.contract.id) != COMPLETED }
                            .keys
                            .toList()
                    }

                    if (farmers.isEmpty()) {
                        edit { content = "**Error:** Could not create a roll call because there are no active farmers that haven’t completed this contract." }
                        return@newSuspendedTransaction
                    }

                    val maxCoopSize: Int = arguments.maxCoopSize
                        ?: if (arguments.contract.maxCoopSize <= 10) arguments.contract.maxCoopSize
                        else (arguments.contract.maxCoopSize * COOP_FILL_PERCENTAGE).roundToInt()

                    val rollCall = createRollCall(arguments.basename, maxCoopSize, farmers)

                    val existingCoops = withProgressBar(
                        goal = rollCall.size,
                        statusText = "Roll call for __${arguments.contract.name}__:\nChecking if co-ops with the given names already exist…"
                    ) {
                        rollCall.keys
                            .mapAsync { coopName -> AuxBrain.getCoopStatus(arguments.contract.id, coopName) }
                            .filterNotNull()
                    }

                    if (existingCoops.isNotEmpty()) {
                        val coopsString = existingCoops.joinToString("\n") { coop ->
                            val validUntil = now().plusSeconds(coop.secondsRemaining.roundToInt())
                            if (validUntil.isAfterNow) {
                                "`${coop.coopId}` (in progress)"
                            } else {
                                "`${coop.coopId}` (ended on ${validUntil.formatYearMonthAndDay()})"
                            }
                        }

                        edit {
                            content = buildString {
                                append("**Error:** ")
                                appendPlural(
                                    existingCoops,
                                    singular = "A co-op already exists for ",
                                    plural = "Co-ops already exist for:\n"
                                )
                                appendLine(coopsString)
                            }
                        }
                        return@newSuspendedTransaction
                    }

                    val coops = withProgressBar(
                        goal = rollCall.size,
                        statusText = "Roll call for __${arguments.contract.name}__:\nCreating co-ops…",
                        unit = "co-ops"
                    ) {
                        rollCall.map { (name, farmers) ->
                            Coop.new {
                                this.contractId = arguments.contract.id
                                this.name = name
                            }.also { coop ->
                                coop.farmers = SizedCollection(farmers)
                                increment()
                            }
                        }.sortedBy(Coop::name)
                    }

                    commit()

                    if (arguments.createRolesAndChannels) withProgressBar(
                        goal = coops.size,
                        statusText = "Roll call for __${arguments.contract.name}__:\nCreating roles and channels…",
                        unit = "co-ops",
                    ) {
                        val coopsCategoryChannel = guildFor(event)?.getChannelOfOrNull<Category>(server.channel.coopsGroup)

                        // No need for async since we'll be rate-limited by Discord
                        coops.forEach createRolesAndChannels@{ coop ->

                            // Create and assign roles
                            setStatusText("Roll call for __${arguments.contract.name}__:\nCreating and assigning roles for `${coop.name}`…")
                            val role = guild?.createRole {
                                name = coop.name
                                mentionable = true
                                color = DEFAULT_ROLE_COLOR
                                reason = "Roll call ${user.asUser().username} through ${this@publicSlashCommand.kord.getSelf().username} for \"${arguments.contract.name}\""
                            }

                            if (role != null) {
                                coop.roleId = role.id
                                coop.farmers.forEach { farmer ->
                                    guild
                                        ?.getMemberOrNull(farmer.discordUser.snowflake)
                                        ?.addRole(role.id, "Roll call for ${arguments.contract.name}")
                                }
                            }

                            // Create channel
                            setStatusText("Roll call for __${arguments.contract.name}__:\nCreating channel and message for `${coop.name}`…")
                            val channel = coopsCategoryChannel?.createTextChannel(coop.name) {
                                topic = "**${coop.name}** vs. __${arguments.contract.name}__"
                                reason = "Roll call ${user.asUser().username} through ${this@publicSlashCommand.kord.getSelf().username} for \"${arguments.contract.name}\""
                            }
                            coop.channelId = channel?.id

                            // Send message in co-op channel
                            channel?.createMessage { content = guild?.coopChannelMessage(coop, role) }

                            commit()
                            increment()
                        }
                    }

                    edit { content = "Roll call for __${arguments.contract.name}__" }

                    multipartRespond(guild?.rollCallResponse(arguments.contract, coops) ?: emptyList())
                }
            }
        }
    }
}
