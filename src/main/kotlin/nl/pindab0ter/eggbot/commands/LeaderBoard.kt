package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.EggBot.emoteSoulEgg
import nl.pindab0ter.eggbot.commands.LeaderBoard.Category.*
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.utilities.*
import nl.pindab0ter.eggbot.utilities.Table.AlignedColumn.Alignment.*
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    init {
        category = FarmersCategory
        name = "leader-board"
        aliases = arrayOf("lb")
        help = "Shows the Earnings Bonus leader board"
        parameters = listOf(
            compactSwitch
        )
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val farmers = transaction {
            Farmer.all().toList().sortedByDescending { it.earningsBonus }
        }

        if (farmers.isEmpty()) "There are no registered farmers".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        formatLeaderBoard(farmers, EARNINGS_BONUS).let { messages ->
            if (event.channel == botCommandsChannel) {
                messages.forEach { message -> event.reply(message) }
            } else {
                event.replyInDms(messages)
            }
        }
    }

    enum class Category {
        EARNINGS_BONUS, SOUL_EGGS, PRESTIGES, DRONE_TAKEDOWNS, ELITE_DRONE_TAKEDOWNS
    }

    fun formatLeaderBoard(
        farmers: List<Farmer>,
        category: Category,
        compact: Boolean = false
    ): List<String> = table {
        val sortedFarmers = when (category) {
            EARNINGS_BONUS -> farmers.sortedByDescending { farmer -> farmer.earningsBonus }
            SOUL_EGGS -> farmers.sortedByDescending { farmer -> farmer.soulEggs }
            PRESTIGES -> farmers.sortedByDescending { farmer -> farmer.prestiges }
            DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.droneTakedowns }
            ELITE_DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.eliteDroneTakedowns }
        }

        title = when (category) {
            EARNINGS_BONUS -> "__**💵 Earnings Bonus**__"
            SOUL_EGGS -> "__**${emoteSoulEgg ?: "🥚"} Soul Eggs**__"
            PRESTIGES -> "__**🥨 Prestiges**__"
            DRONE_TAKEDOWNS -> "__**✈🚫 Drone Takedowns**__"
            ELITE_DRONE_TAKEDOWNS -> "__**🎖✈🚫 Elite Drone Takedowns**__"
        }
        displayHeader = true
        incrementColumn(":")
        column {
            header = "Name"
            leftPadding = 1
            cells = sortedFarmers.map { farmer -> farmer.inGameName }
        }
        column {
            header = when (category) {
                EARNINGS_BONUS -> "Earnings Bonus  "
                SOUL_EGGS -> "Soul Eggs"
                PRESTIGES -> "Prestiges"
                DRONE_TAKEDOWNS -> "Drone Takedowns"
                ELITE_DRONE_TAKEDOWNS -> "Elite Drone Takedowns"
            }
            alignment = RIGHT
            cells = when (category) {
                EARNINGS_BONUS -> farmers.map { farmer -> farmer.earningsBonus.asIllions(rounded = false) + "\u00A0%" }
                SOUL_EGGS -> farmers.map { farmer -> farmer.soulEggs.formatInteger() }
                PRESTIGES -> farmers.map { farmer -> farmer.prestiges.formatInteger() }
                DRONE_TAKEDOWNS -> farmers.map { farmer -> farmer.droneTakedowns.formatInteger() }
                ELITE_DRONE_TAKEDOWNS -> farmers.map { farmer -> farmer.eliteDroneTakedowns.formatInteger() }
            }
        }
        if (category == EARNINGS_BONUS) column {
            leftPadding = 2
            cells = sortedFarmers.map { farmer -> farmer.earningsBonus.asFarmerRole() }
        }
    }.splitMessage(prefix = "```", postfix = "```")
}
