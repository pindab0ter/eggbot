package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteErrorCode

fun pingPong(event: MessageReceivedEvent, arguments: List<String> = emptyList()) =
    event.channel.sendMessage("Pong!").queue()

fun registerFarmer(event: MessageReceivedEvent, arguments: List<String>) {
    if (arguments.size != 1) {
        event.channel.sendMessage("Usage: ${prefix}register <ingame name>").queue()
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
            exception.message?.contains(ColumnNames.farmerDiscordTag) == true) {
            event.channel.sendMessage("You are already registered!").queue()
        } else event.channel.sendMessage("Failed to register.").queue()
    }
}

fun addFarmer(event: MessageReceivedEvent, arguments: List<String>) {
    if (arguments.size != 3) {
        event.channel.sendMessage("Usage: ${prefix}addFarmer inGameName discordTag orderOfMagnitude").queue()
        return
    }

    try {
        transaction {
            Farmer.new {
                inGameName = arguments[0]
                discordTag = arguments[1]
                role = arguments[2]
            }
        }.apply {
            event.channel.sendMessage(
                "Added new farmer:\n" +
                        "\tIn-game name:\t$inGameName\n" +
                        "\tDiscord tag:\t\t$discordTag\n" +
                        "\tRole:\t\t\t\t\t$role"
            ).queue()
        }
    } catch (exception: ExposedSQLException) {
        event.channel.sendMessage("Failed to add farmer.").queue()
    }
}
