package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Parameter
import com.martiansoftware.jsap.Switch
import com.martiansoftware.jsap.UnflaggedOption
import nl.pindab0ter.eggbot.jda.ArgumentCommand

object Test : ArgumentCommand() {

    init {
        name = "test"
        help = "Command for testing and development purposes. None of these arguments will have any effect."
        hidden = true
        guildOnly = false
    }

    private const val PLAYER_NAME = "player name"
    private const val AVAILABLE = "available"

    override val parameters: List<Parameter> = listOf(
        Switch(AVAILABLE)
            .setShortFlag('a')
            .setLongFlag("available")
            .setHelp("set this player as available"),
        UnflaggedOption(PLAYER_NAME)
            .setRequired(REQUIRED)
            .setHelp("name of the player")
    )

    override fun execute(event: CommandEvent, arguments: JSAPResult) {
        val playerName = arguments.getString(PLAYER_NAME)
        val available = arguments.getBoolean(AVAILABLE, false)

        event.reply("$playerName is ${if (available) "" else "not "}available.")
    }
}
