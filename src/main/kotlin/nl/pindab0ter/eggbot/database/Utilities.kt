package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun connectToDatabase() {
    Database.connect("jdbc:sqlite:./EggBot.sqlite", driver = "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
}

fun initializeDatabase() = transaction {
    SchemaUtils.create(Contracts)
    SchemaUtils.create(Goals)
    SchemaUtils.create(DiscordUsers)
    SchemaUtils.create(InGameNames)
    SchemaUtils.create(Coops)
    SchemaUtils.create(FarmerCoops)
}
