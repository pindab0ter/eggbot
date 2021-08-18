package nl.pindab0ter.eggbot.kord.commands

import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.annotation.KordPreview

class ContractArguments : Arguments() {
}

@KordPreview
val contractCommand: suspend SlashCommand<out ContractArguments>.() -> Unit = {
    name = "contract"
    description = ""
    autoAck = PUBLIC

    check {
    }

    action {
    }

    subCommand {
        name = "add"
        description = ""

        check {

        }

        action {
            // TODO: Create channels
        }
    }

    subCommand {
        name = "remove"
        description = ""

        check {

        }

        action {
            // TODO: Remove channels
        }
    }
}
