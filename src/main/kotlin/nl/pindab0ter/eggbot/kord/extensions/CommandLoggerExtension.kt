package nl.pindab0ter.eggbot.kord.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.optional.Optional
import dev.kord.core.cache.data.OptionData
import dev.kord.core.entity.interaction.*
import dev.kord.core.event.interaction.InteractionCreateEvent
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

@KordPreview
class CommandLoggerExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        event<InteractionCreateEvent> {
            action {
                logger.trace { "${event.interaction.userName()}: ${event.interaction.induceUserInput()}" }
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
                is GuildInteraction -> runBlocking { append(member.asMember().username) }
                is ButtonInteraction, is SelectMenuInteraction -> Unit
            }
        }

        /**
         * Get the input the user gave for this interaction, reconstructed.
         */
        @KordPreview
        fun Interaction.induceUserInput(): String = buildString {
            data.data.run {
                when (name) {
                    is Optional.Value -> append("/${name.value} ")
                    else -> return@buildString
                }
                append(options.value?.joinToString(" ") { option: OptionData ->
                    when {
                        option.value is Optional.Value -> "${option.value.value!!.name}: ${option.value.value!!.value}"
                        option.values is Optional.Value -> option.values.value!!.joinToString(" ") { commandArgument ->
                            "${commandArgument.name}: ${commandArgument.value}"
                        }
                        else -> ""
                    }
                })
            }
        }
    }
}
