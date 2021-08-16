package nl.pindab0ter.eggbot.helpers

import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import dev.kord.common.annotation.KordPreview

@OptIn(KordPreview::class)
suspend fun <T : Arguments> SlashCommandContext<T>.publicMultipartFollowUp(messages: List<String>) {
    publicFollowUp {
        content = messages.first()
    }
    messages.tail().forEach { message ->
        channel.createMessage(message)
    }
}

enum class DisplayMode : ChoiceEnum {
    COMPACT, EXTENDED;

    override val readableName: String
        get() = name.lowercase()
}
