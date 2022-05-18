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
        // postgres://<username>:<password>@<host>:<port>/<database>
        val postgresConnectionUrl = env(server.databaseEnv)

        val url = "jdbc:postgresql://${postgresConnectionUrl.substringAfter("@")}"
        val username: String = postgresConnectionUrl.substringAfter("//").substringBefore(":")
        val password: String = postgresConnectionUrl.substringAfter(":").substringAfter(":").substringBefore("@")

        Flyway.configure()
            .dataSource(url, username, password)
            .load().also { flyway ->
                try {
                    flyway.migrate()
                } catch (exception: FlywayValidateException) {
                    flyway.repair()
                    flyway.migrate()
                }
            }

        databases[server.name] = Database.connect(
            url = postgresConnectionUrl.replace("postgres://", "jdbc:postgresql://"),
            driver = "org.postgresql.Driver"
        )
    }

    logger.info { "Connected to database" }
}
