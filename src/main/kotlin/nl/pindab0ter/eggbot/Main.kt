@file:OptIn(KordPreview::class)

package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.extensions.*

lateinit var eggBot: ExtensibleBot

suspend fun main() {
    eggBot = ExtensibleBot(
        token = config.botToken
    ) {
        extensions {
            configureSentry()

            // Extensions
            add(::CommandLogger)

            // Commands
            add(::RegisterCommand)
            add(::ActivityCommand)
            add(::AddCoopCommand)
            add(::ContractsCommand)
            add(::CoopInfoCommand)
            add(::CoopsInfoCommand)
            add(::EarningsBonusCommand)
            add(::LeaderBoardCommand)
            add(::RemoveCoopCommand)
            add(::RemoveCoopsCommand)
            add(::RollCallCommand)
            add(::UnregisterCommand)
            add(::WhoIsCommand)

            // Test command
            if (envOrNull("ENVIRONMENT") != "production") {
                add(::TestCommand)
            }
        }

        hooks {
            beforeExtensionsAdded {
                connectToDatabase()
            }

            beforeStart {
                startScheduler()
            }
        }
    }

    eggBot.start()
}
