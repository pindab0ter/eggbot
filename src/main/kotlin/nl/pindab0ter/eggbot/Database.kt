package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.utils.env
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.jetbrains.exposed.sql.Database

internal fun connectToDatabase() {
    val logger = KotlinLogging.logger {}
    Flyway.configure()
        .dataSource(env("DATABASE_URL"), env("DATABASE_USER"), env("DATABASE_PASSWORD"))
        .load().apply {
            try {
                migrate()
            } catch (exception: FlywayValidateException) {
                repair()
                migrate()
            }
        }

    Database.connect(
        url = env("DATABASE_URL"),
        driver = "org.postgresql.Driver",
        user = env("DATABASE_USER"),
        password = env("DATABASE_PASSWORD")
    )

    logger.info { "Connected to database" }
}
