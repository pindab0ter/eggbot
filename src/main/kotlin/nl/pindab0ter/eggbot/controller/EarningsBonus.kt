package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.helpers.COMPACT
import nl.pindab0ter.eggbot.helpers.compactSwitch
import nl.pindab0ter.eggbot.helpers.extendedSwitch
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.view.earningsBonusResponse
import org.jetbrains.exposed.sql.transactions.transaction

object EarningsBonus : EggBotCommand() {

    private val log = KotlinLogging.logger { }
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
                if (backup == null) "Could not get information on ${farmer.inGameName}".let {
                    event.replyWarning(it)
                    log.warn { it }
                    return@getFarmerBackup
                }

                GlobalScope.launch(Dispatchers.IO) {
                    transaction { farmer.update(backup) }
                }
            }

            earningsBonusResponse(farmer, compact, extended).forEachIndexed { index, block ->
                if (event.channel == EggBot.botCommandsChannel) {
                    event.reply(block)
                } else {
                    event.replyInDm(block) {
                        if (event.isFromType(ChannelType.TEXT) && index == 0) {
                            event.reactSuccess()
                        }
                    }
                }
            }
        }
    }
}
