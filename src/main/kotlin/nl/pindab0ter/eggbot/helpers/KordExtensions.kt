package nl.pindab0ter.eggbot.helpers

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalEnumChoice
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.entity.optional.firstOrNull
import nl.pindab0ter.eggbot.converters.contract
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun <A : Arguments> PublicSlashCommandContext<A>.multipartRespond(messages: List<String>) {
    respond {
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

fun Arguments.displayModeChoice() = optionalEnumChoice<DisplayMode> {
    name = "display-mode"
    description = "Use compact to better fit mobile devices or extended to show numbers in non-scientific notation."
    typeName = DisplayMode::name.name
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

fun Arguments.coopId() = string {
    name = "coop"
    description = "The co-op ID. Can be found in #roll-call or in-game."

    autoComplete {
        val contractName = it.interaction.data.data.options.firstOrNull { optionData ->
            optionData.name == "contract"
        }?.value?.value?.value as String?

        if (contractName !== null) {
            val coops = transaction {
                val contract = AuxBrain.getContracts().find { contract -> contract.name == contractName }
                if (contract != null) {
                    Coop.find { Coops.contractId eq contract.id }.associate { coop -> Pair(coop.name, coop.name) }
                } else {
                    emptyMap()
                }
            }
            suggestStringMap(coops)
        }
    }

    validate {
        failIf(value.contains(" "), "Co-op ID cannot contain spaces.")
    }
}

fun Arguments.createRole() = defaultingBoolean {
    name = "create-role"
    description = "Whether to create a role for this co-op. Defaults to ‘True’."
    defaultValue = true
}

fun Arguments.createChannel() = defaultingBoolean {
    name = "create-channel"
    description = "Whether to create a channel for this co-op. Defaults to ‘True’."
    defaultValue = true
}
