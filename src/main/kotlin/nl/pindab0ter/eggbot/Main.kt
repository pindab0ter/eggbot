package nl.pindab0ter.eggbot

import com.jagrosh.jdautilities.command.CommandClientBuilder
import net.dv8tion.jda.core.JDABuilder
import nl.pindab0ter.eggbot.commands.ContractIDs
import nl.pindab0ter.eggbot.commands.HighScore
import nl.pindab0ter.eggbot.commands.Register
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.database.connectToDatabase
import nl.pindab0ter.eggbot.database.initializeDatabase
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    connectToDatabase()
    initializeDatabase()
    clearDatabase()
    connectClient()
}

val botToken: String = System.getenv("bot_token")
val ownerId: String = System.getenv("owner_id")

private fun connectClient() {
    requireNotNull(botToken) { "Please enter the bot token in the \"bot_token\" environment variable" }
    requireNotNull(ownerId) { "Please enter the owner id in the \"owner_id\" environment variable" }

    val client = CommandClientBuilder()
        .setOwnerId(ownerId)
        .setPrefix("!")
        // TODO: Customize help message
        .useHelpBuilder(true)
        .addCommands(
            ContractIDs,
            HighScore,
            Register
        )
        .build()

    JDABuilder(botToken)
        .addEventListener(client)
        .build()
        .awaitReady()
}

fun clearDatabase() {
    transaction {
        DiscordUser.all().forEach(DiscordUser::delete)
        Farmer.all().forEach(Farmer::delete)
    }
}
