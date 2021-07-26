package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.kord.extensions.CommandLoggerExtension
import nl.pindab0ter.eggbot.kord.extensions.EggBotExtension
import nl.pindab0ter.eggbot.model.Config

@KordPreview
suspend fun main() {
    connectToDatabase()
    startScheduler()

    ExtensibleBot(Config.botToken) {
        extensions {
            add(::CommandLoggerExtension)
            add(::EggBotExtension)
        }

        slashCommands {
            enabled = true
            // defaultGuild = Config.guild
        }
    }.start()
}
