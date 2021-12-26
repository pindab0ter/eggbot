package nl.pindab0ter.eggbot.commands.groups

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashGroup
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.formatMonthAndDay
import nl.pindab0ter.eggbot.helpers.formatDaysAndHoursUntil
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime.now

class SetInactiveArguments : Arguments() {
    val days by int("days", "How many days you want to say yourself inactive as.")
}

@KordPreview
val selfActivityGroup: suspend SlashGroup.() -> Unit = {
    description = "Manage your (in)activity."

    ephemeralSubCommand {
        name = "status"
        description = "Get your activity status."

        lateinit var discordUser: DiscordUser

        check {
            discordUser = transaction { DiscordUser.findById(event.interaction.user.id.asString) }
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

    ephemeralSubCommand(::SetInactiveArguments) {
        name = "set-inactive"
        description = "Set yourself as inactive."

        lateinit var discordUser: DiscordUser

        check {
            discordUser = transaction { DiscordUser.findById(event.interaction.user.id.asString) }
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
            discordUser = transaction { DiscordUser.findById(event.interaction.user.id.asString) }
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