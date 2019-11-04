package nl.pindab0ter.eggbot.utilities

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.jda.commandClient
import nl.pindab0ter.eggbot.simulation.ContractSimulation
import org.joda.time.Duration

fun String.splitMessage(
    prefix: String = "",
    postfix: String = "",
    separator: Char = '\n'
): List<String> = split(separator)
    .also { lines -> require(lines.none { it.length >= 2000 }) { "Any block cannot be larger than 2000 characters." } }
    .fold(listOf("")) { acc, section ->
        if ("${acc.last()}$section$postfix$separator".length < 2000) acc.replaceLast { "$it$section$separator" }
        else acc.replaceLast { "$it$postfix" }.plus("$prefix$section$separator")
    }
    .replaceLast { "$it$postfix" }

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

val Command.missingArguments get() = "Missing argument(s). Use `${commandClient.textualPrefix}${this.name} ${this.arguments}` without the brackets."
val Command.tooManyArguments get() = "Too many arguments. Use `${commandClient.textualPrefix}${this.name} ${this.arguments}` without the brackets."

fun StringBuilder.appendGoalTable(
    simulation: ContractSimulation,
    compact: Boolean
) {
    val eggEmote = Config.eggEmojiIds[simulation.egg]?.let { id ->
        EggBot.jdaClient.getEmoteById(id)?.asMention
    } ?: "🥚"
    append("__$eggEmote **Goals** (${simulation.goalReachedMoments.count { it.moment != null }}/${simulation.goals.count()}):__ ```")
    simulation.goalReachedMoments
        .forEachIndexed { index, (goal, moment) ->
            val success = moment != null && moment < simulation.timeRemaining

            append("${index + 1}. ")
            append(if (success) "✓︎ " else "✗ ")
            appendPaddingCharacters(
                goal.formatIllions(true),
                simulation.goalReachedMoments
                    .filter { simulation.projectedEggs < it.target }
                    .map { it.target.formatIllions(rounded = true) }
            )
            append(goal.formatIllions(true))
            append(" │ ")
            when (moment) {
                null -> append("More than a year")
                Duration.ZERO -> append("Goal reached!")
                else -> append(moment.asDaysHoursAndMinutes(compact))
            }
            if (index + 1 < simulation.goals.count()) appendln()
        }
    appendln("```")
}
