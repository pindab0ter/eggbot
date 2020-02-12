package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.utilities.PrerequisitesCheckResult
import nl.pindab0ter.eggbot.utilities.arguments
import nl.pindab0ter.eggbot.utilities.checkPrerequisites
import org.jetbrains.exposed.sql.transactions.transaction

object Unregister : Command() {
    private val log = KotlinLogging.logger { }

    init {
        name = "unregister"
        arguments = "<discord tag>"
        help = "Unregister a user. This cannot be undone!"
        category = AdminCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 1,
            maxArguments = 2
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val tag = event.arguments.first()

        transaction {
            val discordUser = DiscordUser.find { DiscordUsers.discordTag like tag }.firstOrNull()
                ?: "No registered users found with Discord tag `${tag}`.".let {
                    event.replyWarning(it)
                    log.debug { it }
                    return@transaction
                }

            discordUser.delete()

            StringBuilder()
                .append("`${discordUser.discordTag}` has been unregistered, along with the ")
                .append(
                    when {
                        discordUser.farmers.count() > 1 ->
                            "farmers `${discordUser.farmers.joinToString(", ") { it.inGameName }}`"
                        else ->
                            "farmer `${discordUser.farmers.first().inGameName}`"
                    }
                )
                .toString().let {
                    event.replyWarning(it)
                    log.debug { it }
                }
        }
    }
}
