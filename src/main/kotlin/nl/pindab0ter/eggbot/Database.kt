package nl.pindab0ter.eggbot

import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import kotlin.system.exitProcess

val databases: MutableMap<String, Database> = mutableMapOf()

internal fun connectToDatabase() {
    val logger = KotlinLogging.logger {}

    config.servers.forEach { server ->
        try {
            Flyway.configure()
                .dataSource(config.databaseUrl + server.databaseName, config.databaseUser, config.databasePassword)
                .load()
                .migrate()

            databases[server.name] = Database.connect(
                url = config.databaseUrl + server.databaseName,
                user = config.databaseUser,
                password = config.databasePassword,
            )
        } catch (exception: Exception) {
            logger.error { exception }
            exitProcess(1)
        }
    }

    logger.info { "Connected to database" }
}
