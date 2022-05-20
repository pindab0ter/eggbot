package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.defaultingEnumChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission.SendMessages
import dev.kord.common.entity.Permission.ViewChannel
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.displayModeChoice
import nl.pindab0ter.eggbot.helpers.forEachAsync
import nl.pindab0ter.eggbot.helpers.multipartRespond
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.LeaderBoard
import nl.pindab0ter.eggbot.model.LeaderBoard.EARNINGS_BONUS
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.leaderboardResponse
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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

    override suspend fun setup() = config.servers.forEach { server ->
        publicSlashCommand(::LeaderBoardArguments) {
            name = "leader-board"
            description = "View leader boards. Defaults to the Earnings Bonus leader board."
            guild(server.snowflake)
            requireBotPermissions(
                ViewChannel,
                SendMessages,
            )

            check {
                failIf("There are no registered farmers.") { transaction(databases[server.name]) { Farmer.count() == 0L } }
            }

            action {
                val farmers: List<Farmer> = newSuspendedTransaction(null, databases[server.name]) {
                    // Update the farmers of the member requesting the leader board
                    DiscordUser.findBy(user.id)?.farmers?.forEachAsync { farmer ->
                        AuxBrain.getFarmerBackup(farmer.eggIncId, databases[server.name])
                    }

                    Farmer.all().sortedByDescending { farmer -> farmer.earningsBonus }
                }

                guild?.leaderboardResponse(
                    farmers = farmers,
                    leaderBoard = arguments.leaderBoard,
                    top = arguments.top?.takeIf { it > 0 },
                    displayMode = arguments.displayMode,
                    server = server,
                )?.let { multipartRespond(it) }
            }
        }
    }
}
