package nl.pindab0ter.eggbot.kord

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.model.Config

@KordPreview
suspend fun main() = ExtensibleBot(Config.botToken) {
    extensions {
        add(::FarmersExtension)
    }

    slashCommands {
        enabled = true
    }
}.start()
