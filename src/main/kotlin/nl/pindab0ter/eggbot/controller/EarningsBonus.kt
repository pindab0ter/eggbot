package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.helpers.COMPACT
import nl.pindab0ter.eggbot.helpers.compactSwitch
import nl.pindab0ter.eggbot.helpers.extendedSwitch
import nl.pindab0ter.eggbot.helpers.timeSinceBackup
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config
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

    override fun execute(event: CommandEvent, parameters: JSAPResult) = transaction {
        val compact = parameters.getBoolean(COMPACT)
        val extended = parameters.getBoolean(EXTENDED)

        val farmers = DiscordUser.findById(event.author.id)?.farmers?.toList()?.sortedBy { it.inGameName }!!

        farmers.forEach { farmer ->
            if (!farmer.inGameId.startsWith("EI"))
                return@forEach event.replyWarning("Please migrate `${farmer.inGameName}` using `${Config.prefix}${Migrate.name}` in a DM to ${event.jda.selfUser.asMention}.")


            val backup = AuxBrain.getFarmerBackup(farmer.inGameId)
                ?: return@forEach event.replyAndLogWarning("Could not get information on `${farmer.inGameName}`")

            farmer.update(backup)

            earningsBonusResponse(
                farmer,
                EarningsBonus(farmer),
                backup.timeSinceBackup,
                compact,
                extended
            ).forEach { response ->
                event.reply(response)
            }
        }
    }
}
