package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.formatDaysAndHoursUntil
import nl.pindab0ter.eggbot.helpers.formatMonthAndDay
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime.now

@KordPreview
class SelfExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() {
        ephemeralSlashCommand {

            name = "self"
            description = "Manage your own account and status"

            guild(Config.guild)

            // TODO: group("status", selfStatusGroup)

            group("activity") {
                description = "Manage your (in)activity."

                ephemeralSubCommand {
                    name = "status"
                    description = "Get your activity status."

                    lateinit var discordUser: DiscordUser

                    check {
                        discordUser = transaction<DiscordUser?> { DiscordUser.findById(event.interaction.user.id.toString()) }
                            ?: return@check fail("You have not registered yet. Please do so using `/register`.")
                    }

                    action {
                        if (discordUser.isActive) respond { content = "You are set as active." }
                        else respond {
                            content = "You are inactive until ${
                                discordUser.inactiveUntil?.formatMonthAndDay()
                            } (${
                                now().formatDaysAndHoursUntil(discordUser.inactiveUntil!!)
                            }) from now"
                        }
                    }
                }

                class SetInactiveArguments : Arguments() {
                    val days by int {
                        name = "days"
                        description = "How many days you want to say yourself inactive as."
                    }
                }

                ephemeralSubCommand(::SetInactiveArguments) {
                    name = "set-inactive"
                    description = "Set yourself as inactive."

                    lateinit var discordUser: DiscordUser

                    check {
                        discordUser = transaction<DiscordUser?> { DiscordUser.findById(event.interaction.user.id.toString()) }
                            ?: return@check fail("You have not registered yet. Please do so using `/register`.")
                    }

                    action {
                        if (arguments.days <= 0) respond { content = "Please enter a number of days greater than 0." }
                        if (arguments.days > 356) respond { content = "Please enter a number of days no greater than 356." }

                        transaction {
                            discordUser.inactiveUntil = now().plusDays(arguments.days)
                        }

                        respond {
                            content = "You are set to be inactive until ${discordUser.inactiveUntil?.formatMonthAndDay()}."
                        }
                    }
                }

                ephemeralSubCommand(::SetInactiveArguments) {
                    name = "set-active"
                    description = "Set yourself as active."

                    lateinit var discordUser: DiscordUser

                    check {
                        discordUser = transaction<DiscordUser?> { DiscordUser.findById(event.interaction.user.id.toString()) }
                            ?: return@check fail("You have not registered yet. Please do so using `/register`.")
                    }

                    action {
                        val wasActive = transaction {
                            val oldValue = discordUser.isActive
                            discordUser.inactiveUntil = null
                            return@transaction oldValue
                        }

                        respond {
                            content = if (wasActive) "You are (no longer) inactive." else "You were already active."
                        }
                    }
                }
            }
        }
    }
}
