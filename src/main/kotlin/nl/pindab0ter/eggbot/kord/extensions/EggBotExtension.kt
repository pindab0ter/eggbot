package nl.pindab0ter.eggbot.kord.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.kord.commands.*

@KordPreview
class EggBotExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        slashCommand(::LeaderBoardArguments, leaderBoardCommand)
        slashCommand(::EarningsBonusArguments, earningsBonusCommand)
        slashCommand(::CoopArguments, coopCommand)
    }
}
