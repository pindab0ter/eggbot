package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.helpers.COMPACT
import nl.pindab0ter.eggbot.helpers.compactSwitch
import nl.pindab0ter.eggbot.helpers.extendedSwitch
import nl.pindab0ter.eggbot.helpers.timeSinceBackup
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.EarningsBonus
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.view.earningsBonusResponse
import org.jetbrains.exposed.sql.transactions.transaction

object EarningsBonus : EggBotCommand() {

    private const val EXTENDED = "extended"

    init {
        category = FarmersCategory
        name = "earnings-bonus"
        aliases = arrayOf("eb")
        help = "Shows your Farmer Role, EB and how much SE or PE till your next rank."
        parameters = listOf(
            compactSwitch,
            extendedSwitch
        )
        sendTyping = true
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val compact = parameters.getBoolean(COMPACT)
        val extended = parameters.getBoolean(EXTENDED)

        val farmers = transaction {
            DiscordUser.findById(event.author.id)?.farmers?.toList()?.sortedBy { it.inGameName }!!
        }

        farmers.forEach { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId) { (backup, _) ->
                if (backup == null) return@getFarmerBackup event.replyAndLogWarning("Could not get information on ${farmer.inGameName}")

                GlobalScope.launch(Dispatchers.IO) {
                    transaction { farmer.update(backup) }
                }

                val earningsBonus = EarningsBonus(farmer)

                earningsBonusResponse(farmer, earningsBonus, backup.timeSinceBackup, compact, extended).forEach { response ->
                    event.reply(response)
                }
            }
        }
    }
}
