@file:OptIn(KordPreview::class)

package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.annotation.KordPreview
import dev.kord.gateway.Intent.*
import nl.pindab0ter.eggbot.extensions.*

lateinit var eggBot: ExtensibleBot

suspend fun main() {
    eggBot = ExtensibleBot(
        token = config.botToken
    ) {
        intents {
            +DirectMessages
            +DirectMessageTyping
            +GuildMessages
            +GuildMessageTyping
        }

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
