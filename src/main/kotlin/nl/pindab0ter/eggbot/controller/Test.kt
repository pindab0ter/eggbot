package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
import com.martiansoftware.jsap.UnflaggedOption
import nl.pindab0ter.eggbot.jda.EggBotCommand

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
        val playerName = parameters.getString(PLAYER_NAME)
        val available = parameters.getBoolean(AVAILABLE, false)

        event.reply("$playerName is ${if (available) "" else "not "}available.")
    }
}
