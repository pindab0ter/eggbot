package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.defaultingEnumChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.displayModeChoice
import nl.pindab0ter.eggbot.helpers.multipartRespond
import nl.pindab0ter.eggbot.model.LeaderBoard
import nl.pindab0ter.eggbot.model.LeaderBoard.EARNINGS_BONUS
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.leaderboardResponse
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class LeaderBoardCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName


    override suspend fun setup() {
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
            val displayMode: DisplayMode? by displayModeChoice()
        }

        publicSlashCommand(::LeaderBoardArguments) {
            name = "leader-board"
            description = "View leader boards. Defaults to the Earnings Bonus leader board."

            lateinit var farmers: List<Farmer>

            check {
                farmers = transaction { Farmer.all().toList().sortedByDescending { it.earningsBonus } }
                failIf("There are no registered farmers.") { farmers.isEmpty() }
            }

            action {
                multipartRespond(
                    leaderboardResponse(
                        farmers = farmers,
                        leaderBoard = arguments.leaderBoard,
                        top = arguments.top?.takeIf { it > 0 },
                        displayMode = arguments.displayMode,
                        context = this,
                    )
                )
            }
        }
    }
}
