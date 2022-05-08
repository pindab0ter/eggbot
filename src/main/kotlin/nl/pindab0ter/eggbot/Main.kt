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
        // add(::AdminExtension)
        add(::CommandLoggerExtension)
        add(::CoopExtension)
        add(::CoopInfoExtension)
        add(::EarningsBonusExtension)
        add(::EarningsBonusExtension)
        add(::LeaderBoardExtension)
        add(::PostExtension)
        add(::RegisterExtension)
        add(::SelfExtension)

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
