package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.NO_ALIAS
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.displayModeChoice
import nl.pindab0ter.eggbot.helpers.forEachAsync
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.DiscordUser
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

            action {
                val discordUser = transaction(databases[server.name]) { DiscordUser.findBy(event.interaction.user.id) }

                if (discordUser == null) {
                    respond { content = "You have not registered yet. Please do so using `/register`." }
                    return@action
                }

                val farmers = transaction(databases[server.name]) { discordUser.farmers.toList() }

                if (farmers.isEmpty()) {
                    respond { content = "You have no Egg, Inc. accounts registered yet. Please do so using `/register`." }
                    return@action
                }

                farmers.forEachAsync { farmer ->
                    when (val backup = AuxBrain.getFarmerBackup(farmer.eggIncId, databases[server.name])) {
                        null -> respond { content = "Could not get information on EggBot user with in-game name: `${farmer.inGameName ?: NO_ALIAS}`" }
                        else -> respond { content = earningsBonusResponse(backup, arguments.displayMode) }
                    }
                }
            }
        }
    }
}
