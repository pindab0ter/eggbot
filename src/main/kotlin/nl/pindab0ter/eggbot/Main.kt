@file:OptIn(KordPreview::class)

package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.annotation.KordPreview
import dev.kord.gateway.Intent.*
import nl.pindab0ter.eggbot.extensions.*

lateinit var eggBot: ExtensibleBot

val guildSpecificCommands = listOf(
    ::ActivityCommand,
    ::AddCoopCommand,
    ::ContractsCommand,
    ::CoopInfoCommand,
    ::CoopsInfoCommand,
    ::EarningsBonusCommand,
    ::LeaderBoardCommand,
    ::RemoveCoopCommand,
    ::RollCallExtension,
    ::UnregisterCommand,
    ::WhoIsCommand,
)

suspend fun main() {
    eggBot = ExtensibleBot(
        token = env("BOT_TOKEN")
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
            guildSpecificCommands.forEach(::add)
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
