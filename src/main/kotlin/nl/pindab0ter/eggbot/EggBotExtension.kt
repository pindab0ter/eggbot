package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.commands.adminCommand
import mu.KotlinLogging
import nl.pindab0ter.eggbot.commands.*

@KordPreview
class EggBotExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        slashCommand(adminCommand)
        slashCommand(::LeaderBoardArguments, leaderBoardCommand)
        slashCommand(::EarningsBonusArguments, earningsBonusCommand)
        slashCommand(::CoopInfoArguments, coopInfoCommand)
    }
}
