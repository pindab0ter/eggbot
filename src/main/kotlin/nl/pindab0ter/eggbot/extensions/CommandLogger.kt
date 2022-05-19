package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
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

@KordPreview
class CommandLogger : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "CommandLoggerExtension"

    override suspend fun setup() {
        event<InteractionCreateEvent> {
            action {
                val guild = event.interaction.data.guildId.value?.let { kord.getGuild(it) } ?: return@action

                val userName = event.interaction.user.asMemberOrNull(guild.id)?.displayName
                    ?: event.interaction.user.asUser().username

                logger.trace { "$userName (${guild.name}): ${event.interaction.userInput()}" }
            }
        }
    }

    companion object {
        /**
         * Reconstruct the user input for this interaction.
         */
        @KordPreview
        fun Interaction.userInput(): String = buildString {
            val guild = runBlocking {
                data.guildId.value?.let { guildId -> kord.getGuild(guildId) }
            }

            fun List<Option>.formatOptions(): String = joinToString(" ") { option ->
                when (option) {
                    is SubCommand -> "${option.name} ${option.options.value?.formatOptions()}"
                    is CommandGroup -> "${option.name} ${option.options.value?.formatOptions()}"
                    is StringArgument -> "${option.name}: ${option.value}"
                    is IntegerArgument -> "${option.name}: ${option.value}"
                    is NumberArgument -> "${option.name}: ${option.value}"
                    is BooleanArgument -> "${option.name}: ${option.value}"
                    is MentionableArgument -> "${option.name}: ${option.value}"
                    is AutoCompleteArgument -> "${option.name}: ${option.value}"
                    is UserArgument -> runBlocking {
                        "${option.name}: ${guild?.getMemberOrNull(option.value)?.displayName?.let { "@${it}" } ?: "(id = ${option.value})"}"
                    }
                    is ChannelArgument -> runBlocking {
                        "${option.name}: ${guild?.getChannelOrNull(option.value)?.name?.let { "#${it}" } ?: "(id = ${option.value})"}"
                    }
                    is RoleArgument -> runBlocking {
                        "${option.name}: ${guild?.getRoleOrNull(option.value)?.name?.let { "@${it}" } ?: "(id = ${option.value})"}"
                    }
                    is AttachmentArgument -> "${option.name}: ${option.value}"
                }
            }

            data.data.run {
                append("/${name.value ?: "[null]"} ")
                append(options.value.orEmpty().joinToString { option: OptionData ->
                    when {
                        option.value is Optional.Value -> "${option.value.value!!.name}: ${option.value.value!!.value}"
                        option.values is Optional.Value -> option.values.value!!.formatOptions()
                        option.subCommands is Optional.Value -> "${name.value} ${option.subCommands.value!!.formatOptions()}"
                        else -> ""
                    }
                })
            }
        }
    }
}
