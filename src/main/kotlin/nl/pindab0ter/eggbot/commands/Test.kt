package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.pindab0ter.eggbot.utilities.ProgressBarUpdater
import kotlin.coroutines.CoroutineContext

object Test : Command(), CoroutineScope {

    private val log = KotlinLogging.logger { }

    init {
        name = "test"
        help = "Command for testing and development purposes"
        hidden = true
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        launch {
            event.channel.sendTyping().queue()

            val message = event.channel.sendMessage("Progress…").complete()
            val total = 250
            val progressBar = ProgressBarUpdater(total, message, true)
            (1..total).forEach { i ->
                progressBar.update(i)
                delay(20)
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
}
