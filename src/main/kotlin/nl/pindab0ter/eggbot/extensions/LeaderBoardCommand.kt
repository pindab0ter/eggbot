package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Backup
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.defaultingEnumChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.LeaderBoard
import nl.pindab0ter.eggbot.model.LeaderBoard.EARNINGS_BONUS
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.leaderboardResponse
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class LeaderBoardCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    class LeaderBoardArguments : Arguments() {
        val top: Int? by optionalInt {
            name = "top"
            description = "How many players to show"
        }
        val leaderBoard: LeaderBoard by defaultingEnumChoice {
            name = "board"
            description = "Which board to show"
            defaultValue = EARNINGS_BONUS
            typeName = LeaderBoard::name.name
        }
        val displayMode: DisplayMode by displayModeChoice()
    }

    override suspend fun setup() {
        for (guild in guilds) publicSlashCommand(::LeaderBoardArguments) {
            name = "leader-board"
            description = "View leader boards. Defaults to the Earnings Bonus leader board."
            guild(guild.id)

            check {
                failIf("There are no registered farmers.") { transaction { Farmer.count() == 0 } }
            }

            action {
                val farmers: List<Backup> = transaction {
                    Farmer.all()
                        .mapNotNull { farmer -> AuxBrain.getFarmerBackup(farmer.eggIncId) }
                        .sortedByDescending { backup -> backup.game?.earningsBonus }
                }

                multipartRespond(
                    guild.leaderboardResponse(
                        farmers = farmers,
                        leaderBoard = arguments.leaderBoard,
                        top = arguments.top?.takeIf { it > 0 },
                        displayMode = arguments.displayMode,
                    )
                )
            }
        }
    }
}
