package nl.pindab0ter.eggbot.helpers

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.defaultingEnumChoice
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.createMessage
import nl.pindab0ter.eggbot.converters.contract

suspend fun <A : Arguments> PublicSlashCommandContext<A>.multipartRespond(messages: List<String>) {
    respond { content = messages.first() }
    messages.tail().forEach { message ->
        channel.createMessage { content = message }
    }
}

enum class DisplayMode : ChoiceEnum {
    COMPACT, REGULAR, EXTENDED;

    override val readableName: String
        get() = name.lowercase()
}

fun Arguments.displayModeChoice() = defaultingEnumChoice<DisplayMode> {
    name = "display-mode"
    description = "Use compact to better fit mobile devices or extended to show numbers in non-scientific notation."
    typeName = DisplayMode::name.name
    defaultValue = DisplayMode.REGULAR
}

fun Arguments.compact() = defaultingBoolean {
    name = "compact"
    description = "Better fit output for mobile devices"
    defaultValue = false
}

fun Arguments.contract(): SingleConverter<Contract> = contract {
    name = "contract"
    description = "Select an Egg, Inc. contract."
}

fun Arguments.createRole() = defaultingBoolean {
    name = "create-role"
    description = "Whether to create a role for this co-op. Defaults to ‘False’."
    defaultValue = false
}

fun Arguments.createChannel() = defaultingBoolean {
    name = "create-channel"
    description = "Whether to create a channel for this co-op. Defaults to ‘False’."
    defaultValue = false
}
