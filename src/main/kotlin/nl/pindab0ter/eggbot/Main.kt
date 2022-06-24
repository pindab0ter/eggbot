@file:OptIn(KordPreview::class)

package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
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
            add(::ContractAttemptsCommand)
            add(::ContractsCommand)
            add(::CoopInfoCommand)
            add(::CoopsInfoCommand)
            add(::EarningsBonusCommand)
            add(::LeaderBoardCommand)
            add(::RemoveCoopCommand)
            add(::RemoveCoopsCommand)
            add(::RollCallCommand)
            add(::UnregisterCommand)
            add(::UpdateLeaderBoardsCommand)
            add(::WhoIsCommand)

            // Test command
            if (config.environment != "production") {
                add(::TestCommand)
            }
        }

        hooks {
            beforeExtensionsAdded {
                connectToDatabase()

                if (config.environment == "production") startScheduler()
            }
        }
    }

    eggBot.start()
}
