package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

object WhoIs : EggBotCommand() {

    private const val USER = "user"

    init {
        category = FarmersCategory
        name = "whois"
        aliases = arrayOf("whothefuckis")
        help = "See which Discord user is registered with the given in-game name or vice versa."
        parameters = listOf(
            UnflaggedOption(USER)
                .setUsageName("in-game name OR discord (nick)name")
                .setRequired(true)
                .setHelp(
                    "The in-game name or Discord (nick)name of who you want to look up. If the name contains" +
                            "spaces, use quotation marks."
                )
        )
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val name = parameters.getString(USER).replace(Regex("""^@?(\w*)(?:#\d{4})?$"""), "$1")

        transaction {
            (DiscordUser.find {
                DiscordUsers.discordTag like "${name}_____"
            }.firstOrNull() ?: guild.getMembersByNickname(name, true).firstOrNull()?.let { discordUser ->
                DiscordUser[discordUser.user.id]
            })?.let { discordUser ->
                val discordUserName = discordUser.discordTag.dropLast(5)
                val nickname = guild.getMemberById(discordUser.discordId)?.nickname
                    ?.let { nickname -> " ($nickname)" } ?: ""
                val farmerNames = discordUser.farmers.joinToString("`, `") { it.inGameName }

                // TODO: No farmers or discord users found by the name of <@!444983923777339407>.

                return@let event.reply("`@$discordUserName$nickname` is registered with: `$farmerNames`")
            }

            Farmer.find { Farmers.inGameName like name }.firstOrNull()?.let { farmer ->
                val discordUserName = farmer.discordUser.discordTag.dropLast(5)
                val nickname = guild.getMemberById(farmer.discordUser.discordId)?.nickname
                    ?.let { nickname -> " ($nickname)" } ?: ""

                return@transaction event.reply("`${farmer.inGameName}` belongs to `@$discordUserName$nickname`")
            }

            event.replyAndLogWarning("No farmers or discord users found by the name of `$name`.")
        }
    }
}

