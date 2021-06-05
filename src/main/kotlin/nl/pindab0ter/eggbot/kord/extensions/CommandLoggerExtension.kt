package nl.pindab0ter.eggbot.kord.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.core.entity.interaction.*
import dev.kord.core.entity.interaction.OptionValue.*
import dev.kord.core.event.interaction.InteractionCreateEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

@KordPreview
class CommandLoggerExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        event<InteractionCreateEvent> {
            action {
                logger.trace { "${event.interaction.userName()}: /${event.interaction.command.input()}" }
            }
        }
    }

    companion object {
        /**
         * Get the username of the user that initiated this interaction.
         */
        fun Interaction.userName(): String = buildString {
            when (this@userName) {
                is DmInteraction -> append(user.username)
                is GuildInteraction -> runBlocking {
                    launch {
                        append(member.asMember().username)
                    }
                }
            }
        }

        /**
         * Get the input the user gave for this interaction, reconstructed.
         */
        @KordPreview
        fun InteractionCommand.input(): String = buildString {
            append("$rootName ")
            if (this@input is SubCommand) append("$name ")
            options.entries.forEach { option ->
                append("${option.key}: ")
                when (val optionValue = option.value) {
                    is RoleOptionValue, is UserOptionValue, is ChannelOptionValue -> TODO()
                    is IntOptionValue, is StringOptionValue, is BooleanOptionValue -> append(optionValue.value)
                }
                if (option != options.entries.last()) append(" ")
            }
        }
    }
}
