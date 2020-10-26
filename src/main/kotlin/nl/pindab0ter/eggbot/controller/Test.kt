package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
import com.martiansoftware.jsap.UnflaggedOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.asyncMap
import nl.pindab0ter.eggbot.helpers.table
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.ProgressBar

object Test : EggBotCommand() {

    private const val PLAYER_NAME = "player name"
    private const val AVAILABLE = "available"

    init {
        name = "test"
        aliases = arrayOf("t", "testing")
        help = "Command for testing and development purposes. None of these arguments will have any effect."
        hidden = true
        guildOnly = false
        parameters = listOf(
            Switch(AVAILABLE)
                .setShortFlag('a')
                .setLongFlag("available")
                .setHelp("Set this player as available."),
            UnflaggedOption(PLAYER_NAME)
                .setRequired(REQUIRED)
                .setDefault("pindab0ter")
                .setHelp("Name of the player.")
        )
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val rawValues = (1..100)
        val message = event.channel.sendMessage("Calculating…").complete()
        runBlocking {
            val progressBar = ProgressBar(rawValues.count(), message, coroutineContext = coroutineContext)
            val values = rawValues.asyncMap {
                delay(it * 100L)
                progressBar.update()
                it
            }
            table {
                displayHeaders = false
                column {
                    cells = values.map { index ->
                        index.toString().padEnd(50, '=')
                    }
                }
            }.forEach { block ->
                event.reply(block)
            }
        }
    }
}