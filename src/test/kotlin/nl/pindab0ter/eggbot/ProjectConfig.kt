package nl.pindab0ter.eggbot

import io.kotest.core.config.AbstractProjectConfig
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object ProjectConfig : AbstractProjectConfig() {
    override val parallelism: Int?
        get() = 2

    override fun beforeAll() {
        Database.connect(
            url = "jdbc:sqlite:./UnitTest.sqlite",
            driver = "org.sqlite.JDBC",
            setupConnection = { connection ->
                connection.createStatement().execute("PRAGMA foreign_keys = ON")
            })
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        EggBot.initializeDatabase()
    }

    override fun afterAll() = transaction {
        SchemaUtils.drop(DiscordUsers, Farmers, Coops, CoopFarmers)
    }
}