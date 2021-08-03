package nl.pindab0ter.eggbot.helpers

import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.behavior.interaction.followUp
import dev.kord.rest.builder.interaction.PublicFollowupMessageCreateBuilder
import mu.KotlinLogging

@OptIn(KordPreview::class)
suspend fun <T : Arguments> SlashCommandContext<T>.publicMultipartFollowUp(messages: List<String>) {
    publicFollowUp {
        content = messages.first()
    }
    messages.tail().forEach { message ->
        channel.createMessage(message)
    }
}

/**
 * Assuming an acknowledgement or response has been sent, send a public follow-up message.
 *
 * This function will throw an exception if no acknowledgement or response has been sent yet, or this interaction
 * has already been interacted with in an ephemeral manner.
 */
@KordPreview
suspend inline fun <T : Arguments> SlashCommandContext<T>.publicWarnAndLog(
    noinline builder: PublicFollowupMessageCreateBuilder.() -> Unit,
) {
    val publicFollowupMessage = (interactionResponse as PublicInteractionResponseBehavior).followUp(builder)
    KotlinLogging
        .logger(publicFollowupMessage.getChannelOrNull()?.data?.name?.value ?: "[Unknown channel]")
        .warn { "#${publicFollowupMessage.message.content}" }
}

enum class DisplayMode : ChoiceEnum {
    REGULAR, COMPACT, EXTENDED;

    override val readableName: String
        get() = name.lowercase()
}
