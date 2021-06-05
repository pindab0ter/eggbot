package nl.pindab0ter.eggbot.kord.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.optional.Optional
import dev.kord.core.entity.interaction.DmInteraction
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.core.event.interaction.InteractionCreateEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.kord.commands.LeaderBoard
import kotlin.math.absoluteValue

val logger = KotlinLogging.logger { }

@KordPreview
class EggBotExtension : Extension() {
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        event<InteractionCreateEvent> {
            action {
                val interaction = event.interaction
                when (interaction) {
                    is DmInteraction -> {
                        logger.warn {
                            "${interaction.user.username}: ${interaction.command.rootName} ${
                                interaction.command.options.entries.joinToString(" ") {
                                    buildString {
                                        append(it.key)
                                        if (it.value is Optional.Value<*>) {
                                            when(val value = it.value) {
                                                is OptionValue.RoleOptionValue -> TODO()
                                                is OptionValue.UserOptionValue -> TODO()
                                                is OptionValue.ChannelOptionValue -> TODO()
                                                is OptionValue.IntOptionValue -> append(value.value.absoluteValue)
                                                is OptionValue.StringOptionValue -> append(value.value)
                                                is OptionValue.BooleanOptionValue -> TODO()
                                            }
                                        }
                                    }
                                }
                            }"
                        }
                    }
                    is GuildInteraction -> TODO()
                }
                logger.warn(interaction.toString())
            }
        }

        slashCommand(LeaderBoard::LeaderBoardArguments, LeaderBoard.command)
    }
}
