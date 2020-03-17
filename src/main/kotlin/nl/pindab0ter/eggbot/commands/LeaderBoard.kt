package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP.*
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.EggBot.emoteSoulEgg
import nl.pindab0ter.eggbot.commands.LeaderBoard.Board.*
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.utilities.*
import nl.pindab0ter.eggbot.utilities.Table.AlignedColumn.Alignment.*
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    private const val TOP = "amount of players"
    private const val BOARD = "board"

    init {
        category = FarmersCategory
        name = "leader-board"
        aliases = arrayOf("lb")
        help = "Shows the Earnings Bonus leader board"
        parameters = listOf(
            FlaggedOption(TOP)
                .setShortFlag('n')
                .setLongFlag("amount")
                .setStringParser(INTEGER_PARSER)
                .setHelp("Specify the amount of players you want to display."),
            FlaggedOption(BOARD)
                .setShortFlag('b')
                .setLongFlag("board")
                .setDefault("earnings-bonus")
                .setStringParser(
                    EnumeratedStringParser.getParser(
                        Board.values().joinToString(";") { category ->
                            "${category.longForm};${category.shortForm}"
                        },
                        false,
                        false
                    )
                )
                .setHelp(
                    "Specify which category leader board you want to see. The categories are:\n${Board.values()
                        .joinToString("\n") { category -> "    • `${category.longForm}` or `${category.shortForm}`" }}"
                )
            // , compactSwitch
        )
        sendTyping = true
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

        val amount = parameters.getIntOrNull(TOP)

        if (amount != null && amount < 1) "Amount of players must be a positive number".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val category = parameters.getStringOrNull(BOARD)?.let { input ->
            Board.getByString(input)
        }

        formatLeaderBoard(farmers, category ?: EARNINGS_BONUS, amount).let { messages ->
            if (event.channel == botCommandsChannel) {
                messages.forEach { message -> event.reply(message) }
            } else {
                event.replyInDms(messages)
            }
        }
    }

    enum class Board {
        EARNINGS_BONUS, SOUL_EGGS, PRESTIGES, DRONE_TAKEDOWNS, ELITE_DRONE_TAKEDOWNS;

        companion object {
            fun getByString(input: String): Board? =
                getByLongForm(input) ?: getByShortForm(input)

            private fun getByLongForm(input: String): Board? =
                Board.values().find { category -> category.longForm == input }

            private fun getByShortForm(input: String): Board? =
                Board.values().find { category -> category.shortForm == input }
        }

        val longForm: String get() = name.toLowerCase().replace('_', '-')
        val shortForm: String get() = name.toLowerCase().split('_').joinToString("") { "${it.first()}" }
    }

    fun formatLeaderBoard(
        farmers: List<Farmer>,
        board: Board,
        amount: Int? = null,
        compact: Boolean = false
    ): List<String> = table {
        val sortedFarmers = when (board) {
            EARNINGS_BONUS -> farmers.sortedByDescending { farmer -> farmer.earningsBonus }
            SOUL_EGGS -> farmers.sortedByDescending { farmer -> farmer.soulEggs }
            PRESTIGES -> farmers.sortedByDescending { farmer -> farmer.prestiges }
            DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.droneTakedowns }
            ELITE_DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.eliteDroneTakedowns }
        }.let { sortedFarmers ->
            if (amount != null) sortedFarmers.take(amount) else sortedFarmers
        }

        title = when (board) {
            EARNINGS_BONUS -> "__**💵 Earnings Bonus Leader Board**__"
            SOUL_EGGS -> "__**${emoteSoulEgg ?: "🥚"} Soul Eggs Leader Board**__"
            PRESTIGES -> "__**🥨 Prestiges Leader Board**__"
            DRONE_TAKEDOWNS -> "__**✈🚫 Drone Takedowns Leader Board**__"
            ELITE_DRONE_TAKEDOWNS -> "__**🎖✈🚫 Elite Drone Takedowns Leader Board**__"
        }
        displayHeader = true
        incrementColumn(":")
        column {
            header = "Name"
            leftPadding = 1
            rightPadding = 2
            cells = sortedFarmers.map { farmer -> farmer.inGameName }
        }
        column {
            header = when (board) {
                EARNINGS_BONUS -> "Earnings Bonus  "
                SOUL_EGGS -> "Soul Eggs"
                PRESTIGES -> "Prestiges"
                DRONE_TAKEDOWNS -> "Drone Takedowns"
                ELITE_DRONE_TAKEDOWNS -> "Elite Drone Takedowns"
            }
            alignment = RIGHT
            cells = when (board) {
                EARNINGS_BONUS -> sortedFarmers.map { farmer -> farmer.earningsBonus.asIllions(rounded = false) + "\u00A0%" }
                SOUL_EGGS -> sortedFarmers.map { farmer -> farmer.soulEggs.formatInteger() }
                PRESTIGES -> sortedFarmers.map { farmer -> farmer.prestiges.formatInteger() }
                DRONE_TAKEDOWNS -> sortedFarmers.map { farmer -> farmer.droneTakedowns.formatInteger() }
                ELITE_DRONE_TAKEDOWNS -> sortedFarmers.map { farmer -> farmer.eliteDroneTakedowns.formatInteger() }
            }
        }
        if (board == EARNINGS_BONUS) column {
            header = "Farmer Role"
            leftPadding = 2
            cells = sortedFarmers.map { farmer -> farmer.earningsBonus.asFarmerRole() }
        }
    }.splitMessage(prefix = "```", postfix = "```")
}
