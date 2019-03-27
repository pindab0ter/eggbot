package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteErrorCode

const val prefix = "!"

interface Command {
    val keyWord: String
    val help: String
    fun execute(event: MessageReceivedEvent)
}

object Help : Command {
    override val keyWord = "help"
    override val help = "$prefix$keyWord - Shows this menu"
    override fun execute(event: MessageReceivedEvent) = event.channel
        .sendMessage(
            EmbedBuilder()
                .setTitle("Available commands")
                .setDescription(commands.joinToString("\n") { it.help })
                .build()
        )
        .queue()
}

object Register : Command {
    override val keyWord = "register"
    override val help = "$prefix$keyWord <in-game name> - Register on this server with your in-game name"

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
