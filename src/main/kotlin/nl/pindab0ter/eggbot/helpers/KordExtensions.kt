package nl.pindab0ter.eggbot.helpers

import com.kotlindiscord.kord.extensions.commands.converters.OptionalConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.slash.converters.impl.optionalEnumChoice
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

fun Arguments.displayModeChoice(): OptionalConverter<DisplayMode?> = optionalEnumChoice(
    displayName = "displaymode",
    description = "Use compact to better fit mobile devices or extended to show numbers in non-scientific notation.",
    typeName = DisplayMode::name.name,
)

fun Arguments.compact() = defaultingBoolean(
    displayName = "compact",
    description = "Better fit output for mobile devices",
    defaultValue = false,
)

fun Arguments.coopId() = string(
    displayName = "coop",
    description = "The co-op ID. Can be found in either `#roll-call` or in-game under \"CO-OP INFO\" in the current egg information screen."
)
