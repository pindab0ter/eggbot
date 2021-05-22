package nl.pindab0ter.eggbot.kord.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.kord.commands.LeaderBoard

@KordPreview
class EggBotExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        slashCommand(LeaderBoard::LeaderBoardArguments, LeaderBoard.command)
    }
}