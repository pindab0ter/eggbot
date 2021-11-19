package nl.pindab0ter.eggbot.utilities

import com.kotlindiscord.kord.extensions.ExtensibleBot
import mu.KotlinLogging
import nl.pindab0ter.eggbot.connectToDatabase
import nl.pindab0ter.eggbot.model.Config
import kotlin.system.exitProcess

suspend fun main() = ExtensibleBot(Config.botToken) {
    val logger = KotlinLogging.logger {}

    hooks {
        beforeStart {
            connectToDatabase()

            // Scratch here

            exitProcess(0)
        }
    }
}.start()

