package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import nl.pindab0ter.eggbot.controller.categories.AdminCategory
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction

object Unregister : EggBotCommand() {

    private const val DISCORD_USER = "discord user"

    init {
        category = AdminCategory
        name = "unregister"
        adminRequired = true
        help = "Unregister a user."
        parameters = listOf(
            UnflaggedOption(DISCORD_USER)
                .setRequired(REQUIRED)
                .setHelp(
                    "The discord tag of the user to be removed. Must be formatted as a _Discord tag_: " +
                            "2-32 characters, followed by a `#`-sign and four digits. E.g.: \"`DiscordUser#1234`\""
                )
        )
        sendTyping = true
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {

        // TODO: Enable @-mentions
        val tag = parameters.getString(DISCORD_USER)

        if (!tag.matches(Regex("""^(?:[^@#:]){2,32}#\d{4}${'$'}"""))) return event.replyAndLogWarning(
            "Not a valid Discord tag. Must be 2-32 characters, followed by a `#`-sign and four digits. E.g.: \"`DiscordUser#1234`\""
        )

        transaction {
            val discordUser = DiscordUser.find { DiscordUsers.discordTag eq tag }.firstOrNull()
                ?: return@transaction event.replyAndLogWarning("No registered users found with Discord tag `${tag}`.")

            event.replyAndLogSuccess(buildString {
                append("`${discordUser.discordTag}` has been unregistered")
                when {
                    discordUser.farmers.count() == 1 ->
                        append(", along with the farmer `${discordUser.farmers.first().inGameName}`")
                    discordUser.farmers.count() > 1 ->
                        append(", along with the farmers `${discordUser.farmers.joinToString(", ") { it.inGameName }}`")
                }
                append(".")
            })

            discordUser.delete()
        }
    }
}
