package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.annotation.KordPreview
import dev.kord.gateway.Intent.*
import nl.pindab0ter.eggbot.extensions.*
import nl.pindab0ter.eggbot.model.Config

@KordPreview
suspend fun main() = ExtensibleBot(Config.botToken) {
    intents {
        +DirectMessages
        +DirectMessageTyping
        +GuildMessages
        +GuildMessageTyping
    }

    extensions {
        add(::CommandLogger)

        add(::ActivityCommand)
        add(::ContractsCommand)
        add(::CoopCommand)
        add(::CoopInfoCommand)
        add(::CoopsInfoCommand)
        add(::EarningsBonusCommand)
        add(::LeaderBoardCommand)
        add(::RegisterCommand)
        add(::RollCallExtension)
        add(::UnregisterCommand)
        add(::WhoIsCommand)

        envOrNull("SENTRY_DSN")?.let { sentryDsn ->
            sentry {
                enable = true
                dsn = sentryDsn
            }
        }
    }

    hooks {
        beforeStart {
            connectToDatabase()
            startScheduler()
        }
    }
}.start()
