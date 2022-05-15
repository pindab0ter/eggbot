package nl.pindab0ter.eggbot

import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.jetbrains.exposed.sql.Database

val databases: MutableMap<String, Database> = mutableMapOf()

internal fun connectToDatabase() {
    val logger = KotlinLogging.logger {}

    config.servers.forEach { server ->
        Flyway.configure()
            .dataSource(
                config.databaseUrl + server.databaseName,
                config.databaseUser,
                config.databasePassword
            )
            .load().apply {
                try {
                    migrate()
                } catch (exception: FlywayValidateException) {
                    repair()
                    migrate()
                }
            }

        databases[server.name] = Database.connect(
            url = config.databaseUrl + server.databaseName,
            driver = "org.postgresql.Driver",
            user = config.databaseUser,
            password = config.databasePassword,
        )
    }

    logger.info { "Connected to database" }
}
