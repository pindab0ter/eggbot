package nl.pindab0ter.eggbot.model

import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.helpers.paddingCharacters
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt


class ProgressBar(
    private val goal: Int,
    private val message: Message,
    coroutineContext: CoroutineContext? = null,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext ?: Dispatchers.Default
    private var running: Boolean = true
    private var counter: AtomicInteger = AtomicInteger(0)
    private var dirty: AtomicBoolean = AtomicBoolean(true)
    private var job: Job

    init {
        require(goal > 0) { "Goal must be at least 1" }
        job = loop()
    }

    private fun loop(): Job = GlobalScope.launch(coroutineContext) {
        message.channel.sendTyping().queue()
        var i = 0
        while (running) when {
            dirty.getAndSet(false) -> {
                message.editMessage(drawProgressBar(counter.get(), goal)).queue()
            }
            else -> {
                if (i++ % 5 == 0) message.channel.sendTyping().queue()
                delay(1000)
            }
        }
        message.editMessage(drawProgressBar(goal, goal)).queue({}, { /* Ignore exception */ })
    }

    fun update() {
        counter.incrementAndGet()
        if (counter.get() >= goal) stop()
        dirty.set(true)
    }

    private fun stop() {
        running = false
    }

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