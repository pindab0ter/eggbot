package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.displayModeChoice
import nl.pindab0ter.eggbot.helpers.multipartRespond
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.earningsBonusResponse
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class EarningsBonusCommand : Extension() {
    override val name: String = javaClass.simpleName

    class EarningsBonusArguments : Arguments() {
        val displayMode: DisplayMode by displayModeChoice()
    }

    override suspend fun setup() = config.servers.forEach { server ->
        publicSlashCommand(::EarningsBonusArguments) {
            name = "earnings-bonus"
            description = "See your Farmer Role, EB and how much SE or PE till your next rank."
            guild(server.snowflake)

            lateinit var discordUser: DiscordUser
            lateinit var farmers: List<Farmer>

            check {
                discordUser = transaction(databases[server.name]) {DiscordUser.findBy(event.interaction.user.id) }
                    ?: return@check fail("You have not registered yet. Please do so using `/register`.")

                farmers = transaction(databases[server.name]) {discordUser.farmers.toList().sortedBy(Farmer::inGameName) }
                failIf("You have no Egg, Inc. accounts associated with your Discord account. Please register one using `/register`.") {
                    farmers.isEmpty()
                }
            }

            action {
                farmers.forEach { farmer ->
                    try {
                        val backup = AuxBrain.getFarmerBackup(farmer.eggIncId)
                        multipartRespond(earningsBonusResponse(backup!!, arguments.displayMode))
                    } catch (e: IllegalStateException) {
                        respond { content = "Could not get information on EggBot user with in-game ID: `${farmer.inGameName}`" }
                    }
                }
            }
        }
    }
}
