package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.sql.transactions.transaction

object WhoIs : EggBotCommand() {

    private val log = KotlinLogging.logger { }
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
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val name = parameters.getString(USER).replace(Regex("""^@?(\w*)(?:#\d{4})?$"""), "$1")

        transaction {
            (DiscordUser.find {
                DiscordUsers.discordTag like "${name}_____"
            }.firstOrNull() ?: EggBot.guild.getMembersByNickname(name, true).firstOrNull()?.let { discordUser ->
                DiscordUser[discordUser.user.id]
            })?.let { discordUser ->
                val discordUserName = discordUser.discordTag.dropLast(5)
                val nickname = EggBot.guild.getMemberById(discordUser.discordId)?.nickname
                    ?.let { nickname -> " ($nickname)" } ?: ""
                val farmerNames = discordUser.farmers.joinToString("`, `") { it.inGameName }

                "`@$discordUserName$nickname` is registered with: `$farmerNames`".let {
                    event.reply(it)
                    return@transaction
                }
            }

            Farmer.find { Farmers.inGameName like name }.firstOrNull()?.let { farmer ->
                val discordUserName = farmer.discordUser.discordTag.dropLast(5)
                val nickname = EggBot.guild.getMemberById(farmer.discordUser.discordId)?.nickname
                    ?.let { nickname -> " ($nickname)" } ?: ""

                "`${farmer.inGameName}` belongs to `@$discordUserName$nickname`".let {
                    event.reply(it)
                    return@transaction
                }
            }


            "No farmers or discord users found by the name of `$name`.".let {
                event.reply(it)
                log.debug { it }
                return@transaction
            }
        }
    }
}

