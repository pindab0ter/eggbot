package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.database.ColumnNames
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteErrorCode

object Register : Command() {
    init {
        name = "register"
        arguments = "<in-game name>"
        help = "Register on this server with your in-game name"
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.args.isBlank()) {
            event.replyWarning("Missing in-game name argument. See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }

        try {
            transaction {
                Farmer.new {
                    discordTag = event.author.asTag
                    inGameName = event.arguments[0]
                }
            }.apply {
                event.replySuccess("Successfully registered, welcome!")
            }
        } catch (exception: ExposedSQLException) {
            // TODO: Check __who__ has registered that name and let the sender know if it was them or not
            if (exception.errorCode == SQLiteErrorCode.SQLITE_CONSTRAINT.code &&
                exception.message?.contains(ColumnNames.FARMER_DISCORD_TAG) == true
            ) {
                event.replyWarning("You are already registered!")
            } else {
                event.replyError("Failed to register.")
            }
        }
    }
}