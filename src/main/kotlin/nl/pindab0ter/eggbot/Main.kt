package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
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
        add(::EarningsBonusCommand)
        add(::LeaderBoardCommand)
        add(::RegisterCommand)
        add(::WhoIsCommand)

        if (Config.sentryDsn != null) sentry {
            enable = true
            dsn = Config.sentryDsn
            environment = if (Config.devMode) "development" else "production"
        }
    }

    hooks {
        beforeStart {
            connectToDatabase()
            startScheduler()
        }
    }
}.start()
