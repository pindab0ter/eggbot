package nl.pindab0ter.eggbot.model

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.kord
import nl.pindab0ter.eggbot.helpers.paddingCharacters
import nl.pindab0ter.eggbot.model.ProgressBar.Companion.drawProgressBar
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.math.roundToInt


@OptIn(ExperimentalContracts::class)
inline fun <T, A : Arguments> PublicSlashCommandContext<A>.withProgressBar(
    goal: Int,
    statusText: String? = null,
    unit: String = "",
    message: Message? = null,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    block: ProgressBar.() -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val progressBarMessage = message ?: runBlocking {
        respond { content = drawProgressBar(0, goal, statusText, unit) }
    }.message

    val progressBar = ProgressBar(goal, statusText, unit, progressBarMessage, coroutineContext)

    val value = try {
        progressBar.block()
    } finally {
        progressBar.stop()
    }

    return value
}

class ProgressBar(
    goal: Int,
    statusText: String? = null,
    unit: String = "",
    internal val message: Message,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) {
    private val value: AtomicInteger = AtomicInteger(0)
    private val goal: AtomicInteger = AtomicInteger(goal)
    private val statusText: AtomicReference<String?> = AtomicReference(statusText)
    private val unit: AtomicReference<String> = AtomicReference(unit)
    private val running: AtomicBoolean = AtomicBoolean(true)
    private val dirty: AtomicBoolean = AtomicBoolean(true)
    private val job: Job
    private val logger = KotlinLogging.logger { }

    init {
        job = loop(coroutineContext)
    }

    private fun loop(coroutineContext: CoroutineContext): Job = kord.launch(coroutineContext) {
        if (goal.get() == 0) running.set(false)
        var i = 0
        while (running.get()) when {
            dirty.getAndSet(false) -> {
                message.edit {
                    content = drawProgressBar(
                        value = value.get(),
                        max = goal.get(),
                        statusText = statusText.get(),
                        unit = unit.get(),
                    )
                }
                i = 0
            }
            else -> {
                if (i >= TIME_OUT) stop()
                else if (i++ % SEND_TYPING_INTERVAL == 0) message.channel.type()
                delay(250)
            }
        }
    }

    fun increment() {
        value.incrementAndGet()
        dirty.set(true)
    }

    fun reset(value: Int = 0, goal: Int, statusText: String? = null, unit: String = "") {
        this.value.set(value)
        this.goal.set(goal)
        this.statusText.set(statusText)
        this.unit.set(unit)
        dirty.set(true)
    }

    fun setValue(value: Int) {
        this.value.set(value)
        dirty.set(true)
    }

    fun setGoal(goal: Int) {
        this.goal.set(goal)
        dirty.set(true)
    }

    fun setStatusText(statusText: String) {
        this.statusText.set(statusText)
        dirty.set(true)
    }

    fun setUnit(unit: String) {
        this.unit.set(unit)
        dirty.set(true)
    }

    fun stop() {
        running.set(false)
    }

    companion object {
        private const val TIME_OUT = 30
        private const val SEND_TYPING_INTERVAL = 8

        fun drawProgressBar(
            value: Int,
            max: Int,
            statusText: String? = null,
            unit: String = "",
            width: Int = 30,
            showSteps: Boolean = true,
            showPercentage: Boolean = true,
        ): String {
            val percentage = (value.toDouble() / max.toDouble() * 100.0).roundToInt()
            val completed = (min(value.toDouble(), max.toDouble()) / max.toDouble() * width.toDouble()).roundToInt()
            val remaining = width - completed

            return buildString {
                if (statusText != null) appendLine(statusText)
                append("`")
                append("▓".repeat(completed))
                append("░".repeat(remaining))
                if (showSteps || showPercentage) appendLine()
                if (showSteps) append("${paddingCharacters(value, max)}$value/$max ")
                if (unit.isNotBlank()) append("$unit ")
                if (showPercentage) append("($percentage%)")
                append("`")
            }
        }
    }
}
