package nl.pindab0ter.eggbot.utilities

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import mu.KotlinLogging
import nl.pindab0ter.eggbot.connectToDatabase
import kotlin.system.exitProcess

suspend fun main() = ExtensibleBot(
    token = env("BOT_TOKEN"),
) {
    val logger = KotlinLogging.logger {}

    hooks {
        beforeExtensionsAdded {
            try {
                connectToDatabase()

                logger.debug { "Scratch here…" }

            } catch (e: Exception) {
                logger.error { e.stackTraceToString() }
            } finally {
                exitProcess(0)
            }
        }
    }
}.start()

