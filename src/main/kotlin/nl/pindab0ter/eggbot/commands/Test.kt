package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.pindab0ter.eggbot.utilities.ProgressBar
import nl.pindab0ter.eggbot.utilities.ProgressBar.WhenDone
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
            val total = 12
            val progressBar = ProgressBar(total, message, WhenDone.PUBLISH_FINAL_UPDATE)
            (1..total).forEach { i ->
                progressBar.update(i)
                log.info { i }
                delay(250)
            }
            event.replySuccess("Done!")
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
}
