package nl.pindab0ter.eggbot.commands

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.database.ColumnNames
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteErrorCode

object Register : Command {
    override val keyWord = "register"
    override val help = "$PREFIX$keyWord <in-game name> - Register on this server with your in-game name"

    override fun execute(event: MessageReceivedEvent) {
        val arguments = event.message.arguments

        if (arguments?.size != 1) {
            event.channel.sendMessage(help).queue()
            return
        }

        try {
            transaction {
                Farmer.new {
                    discordTag = event.author.asTag
                    inGameName = arguments[0]
                }
            }.apply {
                event.channel.sendMessage("Successfully registered, welcome!").queue()
            }
        } catch (exception: ExposedSQLException) {
            if (exception.errorCode == SQLiteErrorCode.SQLITE_CONSTRAINT.code &&
                exception.message?.contains(ColumnNames.FARMER_DISCORD_TAG) == true
            ) {
                event.channel.sendMessage("You are already registered!").queue()
            } else {
                event.channel.sendMessage("Failed to register.").queue()
            }
        }
    }
}