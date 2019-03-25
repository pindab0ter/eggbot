package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction

fun pingPong(event: MessageReceivedEvent, arguments: List<String> = emptyList()) =
    event.channel.sendMessage("Pong!").queue()

fun addFarmer(event: MessageReceivedEvent, arguments: List<String>) {
    if (arguments.size != 3) {
        event.channel.sendMessage("Usage: ${prefix}addFarmer inGameName discordTag orderOfMagnitude")
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
