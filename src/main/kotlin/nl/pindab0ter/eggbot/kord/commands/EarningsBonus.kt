package nl.pindab0ter.eggbot.kord.commands

import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingEnum
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.publicMultipartFollowUp
import nl.pindab0ter.eggbot.helpers.publicWarnAndLog
import nl.pindab0ter.eggbot.helpers.timeSinceBackup
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.EarningsBonus
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.earningsBonusResponse
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
object EarningsBonus {
    class EarningsBonusArguments : Arguments() {
        // TODO: Currently as of kord-extensions v1.4.1 results in an Invalid Request Body error
        // val displayMode: DisplayMode by defaultingEnum(
        //     displayName = "display mode",
        //     description = "Use compact to better fit mobile devices or extended to show numbers in non-scientific notation.",
        //     defaultValue = DisplayMode.REGULAR,
        //     typeName = DisplayMode::name.name,
        // )
    }

    val command: suspend SlashCommand<out EarningsBonusArguments>.() -> Unit = {
        name = "earnings-bonus"
        description = "Shows your Farmer Role, EB and how much SE or PE till your next rank."
        autoAck = PUBLIC

        action {
            val farmers = transaction {
                DiscordUser.findById(event.interaction.user.id.asString)
                    ?.farmers
                    ?.toList()
                    ?.sortedBy(Farmer::inGameName)
                    .orEmpty()
            }

            if (farmers.isEmpty()) publicWarnAndLog {
                content = "You have no farmers associated with your Discord account. Please register using `/register`."
            }

            farmers.forEach { farmer ->
                val backup = AuxBrain.getFarmerBackup(farmer.inGameId) ?: return@forEach publicWarnAndLog {
                    content = "Could not get information on `${farmer.inGameName}`"
                }

                publicMultipartFollowUp(earningsBonusResponse(
                    farmer,
                    EarningsBonus(farmer),
                    backup.timeSinceBackup,
                    DisplayMode.REGULAR
                    // arguments.displayMode
                ))
            }
        }
    }
}