package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
import com.martiansoftware.jsap.UnflaggedOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.asyncMap
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

    override fun execute(event: CommandEvent, parameters: JSAPResult) = runBlocking {
        val message = event.channel.sendMessage("Calculating…").complete()
        runBlocking {
            val progressBar = ProgressBar(
                2,
                message,
                "Counting…",
                "number(s) counted",
                coroutineContext = coroutineContext)
            (1..2).asyncMap {
                delay(it * 1000L)
                progressBar.increment()
            }
            progressBar.reset(
                3,
                "Counting again…",
                "number(s) counted"
            )
            (1..3).asyncMap {
                delay(it * 1000L)
                progressBar.increment()
            }
            progressBar.stop()
            event.reply("Done")
        }
    }
}