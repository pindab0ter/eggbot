package nl.pindab0ter.eggbot.model

import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.helpers.paddingCharacters
import nl.pindab0ter.eggbot.model.ProgressBar.WhenDone.PUBLISH_FINAL_UPDATE
import nl.pindab0ter.eggbot.model.ProgressBar.WhenDone.STOP_IMMEDIATELY
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class ProgressBar(
    private val goal: Int,
    private val message: Message,
    private val whenDone: WhenDone = PUBLISH_FINAL_UPDATE
) : CoroutineScope {
    enum class WhenDone { STOP_IMMEDIATELY, PUBLISH_FINAL_UPDATE }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
    private var running: Boolean = true
    private var counter: AtomicInteger = AtomicInteger(0)
    private var dirty: Boolean = true
    private var job: Job

    init {
        job = loop()
    }

    private fun loop(): Job = GlobalScope.launch {
        message.channel.sendTyping().queue()
        var i = 0
        while (running) when {
            dirty -> {
                message.editMessage(drawProgressBar(counter.get(), goal)).queue()
                dirty = false
            }
            else -> {
                if (i++ % 5 == 0) message.channel.sendTyping().queue()
                delay(1000)
            }
        }
        message.editMessage(drawProgressBar(goal, goal)).complete()
    }

    fun update() {
        if (counter.incrementAndGet() >= goal) stop()
    }

    private fun stop(onStop: WhenDone = whenDone): Unit = when (onStop) {
        PUBLISH_FINAL_UPDATE -> running = false
        STOP_IMMEDIATELY -> job.cancel()
    }

    private fun drawProgressBar(
        current: Int,
        total: Int,
        width: Int = 30,
        showSteps: Boolean = true,
        showPercentage: Boolean = true
    ): String {
        val percentage = (current.toDouble() / total.toDouble() * 100.0).roundToInt()
        val full = (current.toDouble() / total.toDouble() * width.toDouble()).roundToInt()
        val empty = width - full

        return buildString {
            append("`")
            append("▓".repeat(full))
            append("░".repeat(empty))
            if (showSteps) append(" ${paddingCharacters(current, total)}$current/$total")
            if (showPercentage) append(" ($percentage%)")
            append("`")
        }
    }
}