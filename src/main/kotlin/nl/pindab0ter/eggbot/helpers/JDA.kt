package nl.pindab0ter.eggbot.helpers

import nl.pindab0ter.eggbot.helpers.Typography.zwsp

fun StringBuilder.appendBreakpoint(): StringBuilder = append(zwsp)

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
    separator: Char = '\n',
): List<String> = split(separator)
    .also { blocks ->
        require(blocks.none { block ->
            block.length >= 2000 - prefix.length - postfix.length
        }) { "Any block cannot be larger than 2000 characters." }
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
fun String.splitCodeBlock(separator: Char = '\n'): List<String> = splitMessage("```", "```", separator)
