package nl.pindab0ter.eggbot.kord.commands

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.slash.converters.impl.optionalEnumChoice
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.displayModeChoice
import nl.pindab0ter.eggbot.model.database.Farmer

@KordPreview
object Coop {
    class CoopArguments : Arguments() {
        val displayMode: DisplayMode? by displayModeChoice()
    }

    val command: suspend SlashCommand<out CoopArguments>.() -> Unit = {
        name = "coop"
        description = "Shows info on a specific co-op, displaying the current status, player contribution and runs a simulation to estimate whether/when the goals will be reached and if people will reach their habitat or transport bottlenecks."

        autoAck = PUBLIC

        lateinit var contract: Contract
        lateinit var farmers: List<Farmer>

        check {

        }

        action {
        }
    }
}
