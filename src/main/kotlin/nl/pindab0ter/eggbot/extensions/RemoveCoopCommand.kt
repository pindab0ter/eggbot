package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.entity.Permission.ManageChannels
import dev.kord.common.entity.Permission.ManageRoles
import dev.kord.common.entity.Snowflake
import dev.kord.rest.request.RestRequestException
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.guilds
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class RemoveCoopCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() {
        for (databaseGuild in guilds) {
            class RemoveCoopArguments : Arguments() {
                val coopId: String by string {
                    name = "name"
                    description = "The co-op ID. Can be found in #roll-call or in-game."

                    validate {
                        failIf(value.contains(" "), "Co-op ID cannot contain spaces.")
                    }

                    autoComplete {
                        val coopInput: String = command.options["name"]?.value as String? ?: ""

                        val coops = transaction {
                            Coop
                                .find { (Coops.name like "$coopInput%") and (Coops.guildId eq databaseGuild.id.toString()) }
                                .limit(25)
                                .orderBy(Coops.name to SortOrder.ASC)
                                .associate { coop -> Pair(coop.name, coop.name) }
                        }
                        suggestStringMap(coops)
                    }
                }
            }

            ephemeralSlashCommand(::RemoveCoopArguments) {
                name = "remove-coop"
                description = "Remove a coop and it's corresponding role and/or channel (does not affect the co-op in-game)"
                requiredPerms += listOf(
                    ManageRoles,
                    ManageChannels,
                )
                guild(databaseGuild.id)

                check {
                    hasRole(Config.adminRole)
                    envOrNull("BOT_OWNER_ID")?.let { botOwnerId ->
                        passIf(event.interaction.user.id == Snowflake(botOwnerId))
                    }
                }

                action {
                    val coop: Coop? = transaction {
                        Coop.find { Coops.name eq arguments.coopId }.firstOrNull()
                    }

                    if (coop == null) {
                        respond { content = "Could not find that co-op" }
                        return@action
                    }

                    val role = coop.roleId?.let { guild?.getRoleOrNull(it) }
                    val channel = coop.channelId?.let { guild?.getChannelOrNull(it) }

                    try {
                        val roleName = role?.name
                        val channelName = channel?.name
                        role?.delete("Removed by ${user.asUser().username} using `/co-op remove`")
                        channel?.delete("Removed by ${user.asUser().username} using `/co-op remove`")
                        transaction { coop.delete() }
                        respond {
                            content = buildString {
                                append("Successfully deleted co-op")
                                if (roleName != null || channelName != null) {
                                    append(" as well as")
                                    if (roleName != null) {
                                        append(" role `@$roleName`")
                                        if (channelName != null) append(" and")
                                    }
                                    if (channelName != null) append(" channel $channelName")
                                }
                            }
                        }
                    } catch (_: RestRequestException) {
                        respond { content = "Could not remove the co-op" }
                    }
                }
            }
        }
    }
}