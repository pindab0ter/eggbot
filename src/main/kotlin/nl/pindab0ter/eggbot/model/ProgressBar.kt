package nl.pindab0ter.eggbot.model

import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.helpers.paddingCharacters
import org.apache.logging.log4j.kotlin.Logging
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt


class ProgressBar(
    goal: Int,
    private var message: Message,
    private var statusText: String? = null,
    private var unit: String = "",
) : Logging {
    private var goal: AtomicInteger = AtomicInteger(goal)
    private var running: AtomicBoolean = AtomicBoolean(true)
    private var counter: AtomicInteger = AtomicInteger(0)
    private var dirty: AtomicBoolean = AtomicBoolean(true)
    private var job: Job

    init {
        job = loop()
    }

    private fun loop(): Job = GlobalScope.launch(Dispatchers.Default) {
        if (goal.get() == 0) running.set(false)
        var i = 0
        while (running.get()) when {
            dirty.getAndSet(false) -> {
                message.editMessage(buildString {
                    if (statusText != null) appendLine(statusText)
                    appendLine(drawProgressBar(counter.get(), goal.get(), unit))
                }).queue()
                i = 0
            }
            else -> {
                if (i >= TIME_OUT) running.set(false)
                if (i++ % SEND_TYPING_INTERVAL == 0) message.channel.sendTyping().queue()
                delay(1000)
            }
        }
        try {
            message.delete().queue()
        } catch (exception: Exception) {
            logger.error("Failed to delete message: ${exception.message}")
        }
    }

    fun increment() {
        counter.incrementAndGet()
        dirty.set(true)
    }

    fun reset(goal: Int, statusText: String? = null, unit: String = "") {
        this.statusText = statusText
        this.unit = unit
        this.goal.set(goal)
        counter.set(0)
        dirty.set(true)
    }

    fun stop() {
        running.set(false)
    }

    companion object {
        private const val TIME_OUT = 30
        private const val SEND_TYPING_INTERVAL = 10

        private fun drawProgressBar(
            current: Int,
            total: Int,
            unit: String = "",
            width: Int = 30,
            showSteps: Boolean = true,
            showPercentage: Boolean = true,
        ): String {
            val percentage = (current.toDouble() / total.toDouble() * 100.0).roundToInt()
            val completed = (current.toDouble() / total.toDouble() * width.toDouble()).roundToInt()
            val remaining = width - completed

            return buildString {
                append("`")
                append("▓".repeat(completed))
                append("░".repeat(remaining))
                if (showSteps || showPercentage) appendLine()
                if (showSteps) append("${paddingCharacters(current, total)}$current/$total ")
                if (unit.isNotBlank()) append("$unit ")
                if (showPercentage) append("($percentage%)")
                append("`")
            }
        }
    }
}