package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.commands.groups.adminCommandGroup
import mu.KotlinLogging
import nl.pindab0ter.eggbot.commands.*
import nl.pindab0ter.eggbot.commands.groups.postGroup

@KordPreview
class SlashCommandsExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "EggBotExtension"

    override suspend fun setup() {
        publicSlashCommand(adminCommandGroup)
        ephemeralSlashCommand(selfGroup)
        publicSlashCommand(::LeaderBoardArguments, leaderBoardCommand)
        publicSlashCommand(::EarningsBonusArguments, earningsBonusCommand)
        publicSlashCommand(::CoopInfoArguments, coopInfoCommand)
        ephemeralSlashCommand(postGroup)
    }
}
