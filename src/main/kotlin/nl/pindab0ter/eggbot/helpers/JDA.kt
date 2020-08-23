package nl.pindab0ter.eggbot.helpers

import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.User
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction

val User.isRegistered: Boolean
    get() = transaction {
        DiscordUser.findById(id)?.farmers?.sortedBy { it.inGameName }?.isNotEmpty() == true
    }

val User.isAdmin: Boolean
    get() = EggBot.guild.getMember(this)?.let { author ->
        author.isOwner || author == EggBot.botOwner || author.roles.contains(EggBot.adminRole)
    } == true

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
    .also { blocks ->
        require(blocks.none { it.length >= 2000 - prefix.length - postfix.length }) { "Any block cannot be larger than 2000 characters." }
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
