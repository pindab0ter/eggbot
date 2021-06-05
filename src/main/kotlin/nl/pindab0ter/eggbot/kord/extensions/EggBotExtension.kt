package nl.pindab0ter.eggbot.kord.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.core.entity.interaction.DmInteraction
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.core.entity.interaction.OptionValue.*
import dev.kord.core.event.interaction.InteractionCreateEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.kord.commands.LeaderBoard
import kotlin.math.absoluteValue


@KordPreview
class EggBotExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        slashCommand(LeaderBoard::LeaderBoardArguments, LeaderBoard.command)
    }
}
