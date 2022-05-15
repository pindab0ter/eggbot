package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import mu.KotlinLogging

internal suspend fun ExtensibleBotBuilder.ExtensionsBuilder.configureSentry() {
    val logger = KotlinLogging.logger {}

    if (!config.sentryDsn.isNullOrBlank()) {
        sentry {
            enable = true
            dsn = config.sentryDsn
        }
        logger.info("Configured Sentry")
    }
}