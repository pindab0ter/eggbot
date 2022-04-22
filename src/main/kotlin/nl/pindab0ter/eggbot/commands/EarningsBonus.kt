package nl.pindab0ter.eggbot.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.displayModeChoice
import nl.pindab0ter.eggbot.helpers.multipartRespond
import nl.pindab0ter.eggbot.helpers.timeSinceBackup
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.EarningsBonus
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.earningsBonusResponse
import org.jetbrains.exposed.sql.transactions.transaction

class EarningsBonusArguments : Arguments() {
    val displayMode: DisplayMode? by displayModeChoice()
}

@KordPreview
val earningsBonusCommand: suspend PublicSlashCommand<out EarningsBonusArguments>.() -> Unit = {
    name = "earnings-bonus"
    description = "Shows your Farmer Role, EB and how much SE or PE till your next rank."

    lateinit var discordUser: DiscordUser
    lateinit var farmers: List<Farmer>

    check {
        discordUser = transaction { DiscordUser.findById(event.interaction.user.id.toString()) }
            ?: return@check fail("You have not registered yet. Please do so using `/register`.")

        farmers = transaction { discordUser.farmers.toList().sortedBy(Farmer::inGameName) }
        failIf("You have no Egg, Inc. accounts associated with your Discord account. Please register one using `/register`.") {
            farmers.isEmpty()
        }
    }

    action {
        farmers.forEach { farmer ->
            when (val backup = AuxBrain.getFarmerBackup(farmer.inGameId)) {
                null -> respond {
                    content = "Could not get information on EggBot user with in-game ID: `${farmer.inGameName}`"
                }
                else -> multipartRespond(
                    earningsBonusResponse(
                        farmer,
                        EarningsBonus(farmer),
                        backup.timeSinceBackup,
                        arguments.displayMode
                    )
                )
            }
        }
    }
}
