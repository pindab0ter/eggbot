package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.missingArguments
import org.jetbrains.exposed.sql.transactions.transaction

object WhoIs : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "whois"
        aliases = arrayOf("who-is", "who", "whothefuckis")
        arguments = "<in-game name>"
        help = "See which Discord user has registered with that in-game name."
        // category = UsersCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        if (event.arguments.isEmpty()) missingArguments.let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        val name = event.arguments.joinToString(" ")
        val farmer = transaction {
            Farmer.find { Farmers.inGameName like name }.firstOrNull()
        }


        if (farmer == null) "No farmer found with name `$name`.".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        val discordUser = transaction { farmer.discordUser }

        event.reply("`${farmer.inGameName}` belongs to `${discordUser.discordTag.dropLast(5)}`")
    }
}
