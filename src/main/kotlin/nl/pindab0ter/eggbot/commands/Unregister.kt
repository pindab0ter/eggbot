package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot.jdaClient
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.jda.EggBotCommand
import org.jetbrains.exposed.sql.transactions.transaction

object Unregister : EggBotCommand() {

    private val log = KotlinLogging.logger { }
    private const val DISCORD_USER = "discord user"

    init {
        category = AdminCategory
        name = "unregister"
        adminRequired = true
        help = "Unregister a user."
        parameters = listOf(
            UnflaggedOption(DISCORD_USER)
                .setRequired(REQUIRED)
                .setHelp("The discord tag of the user to be removed. Must be formatted as a _Discord tag_; ")
        )
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        event.channel.sendTyping().queue()

        // TODO: Enable @-mentions
        val tag = parameters.getString(DISCORD_USER)

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
