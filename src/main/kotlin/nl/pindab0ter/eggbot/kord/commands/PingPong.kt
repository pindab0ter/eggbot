package nl.pindab0ter.eggbot.kord

import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand

object PingPong {
    val command: suspend SlashCommand<out Arguments>.() -> Unit = {
        name = "ping"
        description = "Ping!"

        action {
            ephemeralFollowUp("Pong!")
        }
    }
}