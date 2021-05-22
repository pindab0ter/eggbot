package nl.pindab0ter.eggbot.helpers

import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import dev.kord.common.annotation.KordPreview

@OptIn(KordPreview::class)
suspend fun <T : Arguments> SlashCommandContext<T>.publicFollowUp(messages: List<String>) {
    publicFollowUp {
        content = messages.first()
    }
    messages.tail().forEach { message ->
        channel.createMessage(message)
    }
}