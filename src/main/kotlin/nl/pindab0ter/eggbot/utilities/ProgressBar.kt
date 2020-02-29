package nl.pindab0ter.eggbot.utilities

import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.utilities.ProgressBar.WhenDone.PUBLISH_FINAL_UPDATE
import nl.pindab0ter.eggbot.utilities.ProgressBar.WhenDone.STOP_IMMEDIATELY
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext

class ProgressBar(
    private val goal: Int,
    private val message: Message,
    private val whenDone: WhenDone = PUBLISH_FINAL_UPDATE
) : CoroutineScope {
    enum class WhenDone { STOP_IMMEDIATELY, PUBLISH_FINAL_UPDATE }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
    private val sendTypingEvents: MutableList<CompletableFuture<Void>> = mutableListOf()
    private var running: Boolean = true
    private var value: Int = 0
        set(value) {
            field = value
            dirty = true
        }
    private var dirty: Boolean = true
    private var job: Job

    init {
        job = loop()
    }

    private fun loop() = GlobalScope.launch {
        while (running) when {
            dirty -> {
                sendTypingEvents.add(message.channel.sendTyping().submit())
                message.editMessage(drawProgressBar(value, goal)).complete()
                dirty = false
            }
            else -> delay(1000)
        }
        message.editMessage(drawProgressBar(goal, goal)).complete()
    }

    fun update(newValue: Int) = when {
        newValue >= goal -> stop()
        else -> value = newValue
    }

    fun stop(onStop: WhenDone = whenDone) {
        when (onStop) {
            PUBLISH_FINAL_UPDATE -> running = false
            STOP_IMMEDIATELY -> job.cancel()
        }
        sendTypingEvents.forEach { completableFuture -> completableFuture.cancel(true) }
    }
}