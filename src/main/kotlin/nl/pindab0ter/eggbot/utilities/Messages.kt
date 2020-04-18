package nl.pindab0ter.eggbot.utilities

import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.entities.ChannelType

/**
 * Splits a string into multiple strings.
 *
 * Split a string at the specified [separator] in order to fit Discord's message limit of 2000 characters.
 * Each element will be surrounded with the specified [prefix] and [postfix].
 *
 * @param prefix String to prepend to each message
 * @param postfix String to append to each message
 * @param separator Character on which to split the string
 *
 * @return A [List] containing strings that don't exceed 2000 length.
 */
fun String.splitMessage(
    prefix: String = "",
    postfix: String = "",
    separator: Char = '\n'
): List<String> = split(separator)
    .also { lines ->
        require(lines.none { it.length >= 2000 - prefix.length - postfix.length }) { "Any block cannot be larger than 2000 characters." }
    }
    .fold(listOf("")) { acc, section ->
        if ("${acc.last()}$section$postfix$separator".length < 2000) acc.replaceLast { "$it$section$separator" }
        else acc.replaceLast { "$it$postfix" }.plus("$prefix$section$separator")
    }

/**
 * Splits a code block string into multiple code blocks.
 *
 * The string will be split at newlines, never exceeding 2000 characters per element.
 */
fun String.splitCodeBlock(): List<String> = splitMessage("```", "```")

fun CommandEvent.replyInDms(messages: List<String>) {
    var successful: Boolean? = null
    messages.forEachIndexed { i, message ->
        replyInDm(message, {
            successful = (successful ?: true) && true
            if (i == messages.size - 1 && isFromType(ChannelType.TEXT)) reactSuccess()
        }, {
            if (successful == null) replyWarning("Help cannot be sent because you are blocking Direct Messages.")
            successful = false
        })
    }
}
