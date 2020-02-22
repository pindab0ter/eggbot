package nl.pindab0ter.eggbot.utilities

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.jda.commandClient
import kotlin.coroutines.CoroutineContext

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

class ProgressBarUpdater(
    private val goal: Int,
    private val message: Message
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default
    private var running: Boolean = true
    private var value: Int = 0
    private var dirty: Boolean = true

    init {
        loop()
    }

    private fun loop() = GlobalScope.launch {
        while (running) {
            if (dirty) {
                message.editMessage(drawProgressBar(value, goal)).queue()
                dirty = false
            }
            if (value >= goal) running = false
            else delay(1000)
        }
        message.editMessage(drawProgressBar(goal, goal)).queue()
    }

    fun update(value: Int) {
        this.value = value
        dirty = true
    }
}
