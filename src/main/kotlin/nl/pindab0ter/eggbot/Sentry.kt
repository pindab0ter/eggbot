package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import mu.KotlinLogging

internal suspend fun ExtensibleBotBuilder.ExtensionsBuilder.configureSentry() {
    val logger = KotlinLogging.logger {}

    if (!envOrNull("SENTRY_DSN").isNullOrBlank()) {
        sentry {
            enable = true
            dsn = env("SENTRY_DSN")
        }
        logger.info("Configured Sentry")
    }
}