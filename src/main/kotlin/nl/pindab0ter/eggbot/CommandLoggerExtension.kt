package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.CommandArgument.*
import dev.kord.common.entity.CommandGroup
import dev.kord.common.entity.Option
import dev.kord.common.entity.SubCommand
import dev.kord.common.entity.optional.Optional
import dev.kord.core.cache.data.OptionData
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.event.interaction.InteractionCreateEvent
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.configuredGuild
import nl.pindab0ter.eggbot.model.Config

@KordPreview
class CommandLoggerExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "CommandLoggerExtension"

    override suspend fun setup() {
        event<InteractionCreateEvent> {
            action {
                logger.trace { "${event.interaction.userName()}: ${event.interaction.userInput()}" }
            }
        }
    }

    companion object {
        /**
         * Get the username of the user that initiated this interaction.
         */
        fun Interaction.userName(): String = runBlocking {
            user.asMemberOrNull(Config.guild)?.displayName ?: user.asUser().username
        }

        /**
         * Reconstruct the user input for this interaction.
         */
        @KordPreview
        fun Interaction.userInput(): String = buildString {
            data.data.run {
                append("/${name.value ?: "[null]"} ")
                append(options.value.orEmpty().joinToString { option: OptionData ->
                    when {
                        option.value is Optional.Value -> "${option.value.value!!.name}: ${option.value.value!!.value}"
                        option.values is Optional.Value -> option.values.value!!.formatOptions()
                        option.subCommands is Optional.Value -> option.subCommands.value!!.formatOptions()
                        else -> ""
                    }
                })
            }
        }

        private fun List<Option>.formatOptions(): String = joinToString(" ") { option ->
            when (option) {
                is SubCommand -> "${option.name} ${option.options.value?.formatOptions()}"
                is CommandGroup -> "${option.name} ${option.options.value?.formatOptions()}"
                is StringArgument -> "${option.name}: ${option.value}"
                is IntegerArgument -> "${option.name}: ${option.value}"
                is NumberArgument -> "${option.name}: ${option.value}"
                is BooleanArgument -> "${option.name}: ${option.value}"
                is UserArgument -> runBlocking {
                    "${option.name}: ${configuredGuild?.getMemberOrNull(option.value)?.displayName?.let { "@${it}" } ?: "(id = ${option.value.asString})"}"
                }
                is ChannelArgument -> runBlocking {
                    "${option.name}: ${configuredGuild?.getChannelOrNull(option.value)?.name?.let { "#${it}" } ?: "(id = ${option.value.asString})"}"
                }
                is RoleArgument -> runBlocking {
                    "${option.name}: ${configuredGuild?.getRoleOrNull(option.value)?.name?.let { "@${it}" } ?: "(id = ${option.value.asString})"}"
                }
                is MentionableArgument -> "${option.name}: ${option.value}"
            }
        }
    }
}
