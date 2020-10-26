package nl.pindab0ter.eggbot.model

import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.exceptions.ContextException
import nl.pindab0ter.eggbot.helpers.paddingCharacters
import org.apache.logging.log4j.kotlin.Logging
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt


class ProgressBar(
    private val goal: Int,
    private val message: Message,
    private val statusText: String? = null,
    coroutineContext: CoroutineContext? = null,
) : CoroutineScope, Logging {
    override val coroutineContext: CoroutineContext = coroutineContext ?: Dispatchers.Default
    private var deleteMessage: AtomicBoolean = AtomicBoolean(true)
    private var running: AtomicBoolean = AtomicBoolean(true)
    private var counter: AtomicInteger = AtomicInteger(0)
    private var dirty: AtomicBoolean = AtomicBoolean(true)
    private var job: Job

    init {
        job = loop()
    }

    private fun loop(): Job = GlobalScope.launch(coroutineContext) {
        if (goal == 0) running.set(false)
        else message.channel.sendTyping().queue()
        var i = 0
        while (running.get()) when {
            dirty.getAndSet(false) -> {
                val contents = buildString {
                    if (statusText != null) appendLine(statusText)
                    appendLine(drawProgressBar(counter.get(), goal))
                }
                message.editMessage(contents).queue()
                i = 0
            }
            else -> {
                if (i >= TIME_OUT) running.set(false)
                if (i++ % SEND_TYPING_INTERVAL == 0) message.channel.sendTyping().queue()
                delay(1000)
            }
        }
        if (deleteMessage.get()) try {
            message.delete().queue()
        } catch (exception: ContextException) {
            logger.error("Failed to delete message: ${exception.localizedMessage}")
        }
    }

    fun update() {
        counter.incrementAndGet()
        if (counter.get() >= goal) stop()
        dirty.set(true)
    }

    fun stop() {
        running.set(false)
    }

    fun stopAndDeleteMessage() {
        deleteMessage.set(true)
        stop()
    }

    companion object {
        private const val TIME_OUT = 30
        private const val SEND_TYPING_INTERVAL = 10

        private fun drawProgressBar(
            current: Int,
            total: Int,
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
                if (showSteps) append(" ${paddingCharacters(current, total)}$current/$total")
                if (showPercentage) append(" ($percentage%)")
                append("`")
            }
        }
    }
}