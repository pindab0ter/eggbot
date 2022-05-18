package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.utils.env
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.jetbrains.exposed.sql.Database

val databases: MutableMap<String, Database> = mutableMapOf()

internal fun connectToDatabase() {
    val logger = KotlinLogging.logger {}

    config.servers.forEach { server ->
        val urlString = env(server.databaseJdbcUrlEnv)
        val user = urlString.substringAfter("user=", "").substringBefore("&")
        val password = urlString.substringAfter("password=", "").substringBefore("&")

        Flyway.configure()
            .dataSource(urlString, user, password)
            .load().also { flyway ->
                try {
                    flyway.migrate()
                } catch (exception: FlywayValidateException) {
                    flyway.repair()
                    flyway.migrate()
                }
            }

        databases[server.name] = Database.connect(
            url = urlString,
            driver = "org.postgresql.Driver"
        )
    }

    logger.info { "Connected to database" }
}
