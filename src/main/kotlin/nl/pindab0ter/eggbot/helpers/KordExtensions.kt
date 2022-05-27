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
import nl.pindab0ter.eggbot.converters.coopContract
import nl.pindab0ter.eggbot.helpers.Plurality.PLURAL
import nl.pindab0ter.eggbot.helpers.Plurality.SINGULAR

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

fun Arguments.coopContract(): SingleConverter<Contract> = coopContract {
    name = "contract"
    description = "Select an Egg, Inc. co-op contract."
}

fun Arguments.createRolesAndChannels(plurality: Plurality) = defaultingBoolean {
    name = when (plurality) {
        SINGULAR -> "create-role-and-channel"
        PLURAL -> "create-roles-and-channels"
    }
    description = when (plurality) {
        SINGULAR -> "Create a role and a channel for the contract. This is the default."
        PLURAL -> "Create roles and channels for the contracts. This is the default."
    }
    defaultValue = true
}
