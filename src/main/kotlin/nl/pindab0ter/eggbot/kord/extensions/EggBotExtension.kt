package nl.pindab0ter.eggbot.kord.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.kord.commands.Coop
import nl.pindab0ter.eggbot.kord.commands.EarningsBonus
import nl.pindab0ter.eggbot.kord.commands.LeaderBoard

@KordPreview
class EggBotExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        slashCommand(LeaderBoard::LeaderBoardArguments, LeaderBoard.command)
        slashCommand(EarningsBonus::EarningsBonusArguments, EarningsBonus.command)
        slashCommand(Coop::CoopArguments, Coop.command)
    }
}
