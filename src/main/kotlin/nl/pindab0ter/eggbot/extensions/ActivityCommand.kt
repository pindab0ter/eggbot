package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.formatDaysAndHoursUntil
import nl.pindab0ter.eggbot.helpers.formatMonthAndDay
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.DiscordUsers
import nl.pindab0ter.eggbot.view.inactivesResponse
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime.now

@KordPreview
class ActivityCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() = config.servers.forEach { server ->
        ephemeralSlashCommand {
            name = "activity"
            description = "Manage your (in)activity."
            guild(server.snowflake)

            ephemeralSubCommand {
                name = "status"
                description = "Get your activity status."

                lateinit var discordUser: DiscordUser

                check {
                    discordUser = transaction(databases[server.name]) {
                        DiscordUser.findBy(event.interaction.user.id)
                    } ?: return@check fail("You have not registered yet. Please do so using `/register`.")
                }

                action {
                    if (discordUser.isActive) respond { content = "You are set as active." }
                    else respond {
                        val inactiveUntilDate = discordUser.inactiveUntil?.formatMonthAndDay()
                        val daysAndHoursUntilActive = now().formatDaysAndHoursUntil(discordUser.inactiveUntil!!)
                        content = "You are inactive until $inactiveUntilDate ($daysAndHoursUntilActive) from now"
                    }
                }
            }

            class SetInactiveArguments : Arguments() {
                val days by int {
                    name = "days"
                    description = "How many days you want to say yourself inactive as."

                    validate {
                        failIf(value <= 0, "You must say yourself inactive for at least 1 day.")
                        failIf(value > 356, "You can't say yourself inactive for more than a year.")
                    }
                }
            }

            ephemeralSubCommand(::SetInactiveArguments) {
                name = "set-inactive"
                description = "Set yourself as inactive for a certain amount of days."

                lateinit var discordUser: DiscordUser

                check {
                    discordUser = transaction(databases[server.name]) { DiscordUser.findById(event.interaction.user.id.toString()) }
                        ?: return@check fail("You have not registered yet. Please do so using `/register`.")
                }


                action {

                    transaction(databases[server.name]) {
                        discordUser.inactiveUntil = now().plusDays(arguments.days)
                    }

                    respond {
                        content = "You are set to be inactive until ${discordUser.inactiveUntil?.formatMonthAndDay()}."
                    }
                }
            }

            ephemeralSubCommand {
                name = "set-active"
                description = "Set yourself as active."

                lateinit var discordUser: DiscordUser

                check {
                    discordUser = transaction(databases[server.name]) { DiscordUser.findById(event.interaction.user.id.toString()) }
                        ?: return@check fail("You have not registered yet. Please do so using `/register`.")
                }

                action {
                    val wasActive = transaction(databases[server.name]) {
                        val oldValue = discordUser.isActive
                        discordUser.inactiveUntil = null
                        return@transaction oldValue
                    }

                    respond {
                        content = if (wasActive) "You are (no longer) inactive." else "You were already active."
                    }
                }
            }

            publicSubCommand {
                name = "list"
                description = "List all currently inactive users."

                action {
                    val inactiveUsers = transaction(databases[server.name]) {
                        DiscordUser
                            .find { DiscordUsers.inactiveUntil.isNotNull() and (DiscordUsers.inactiveUntil greater now()) }
                            .orderBy(DiscordUsers.inactiveUntil to ASC)
                            .toList()
                    }

                    respond {
                        content = guild.inactivesResponse(inactiveUsers)
                    }
                }
            }
        }
    }
}
