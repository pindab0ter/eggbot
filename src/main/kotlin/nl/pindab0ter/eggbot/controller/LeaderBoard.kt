package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP.INTEGER_PARSER
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.EggBot.emoteProphecyEgg
import nl.pindab0ter.eggbot.EggBot.emoteSoulEgg
import nl.pindab0ter.eggbot.controller.LeaderBoard.Board.*
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : EggBotCommand() {

    private const val TOP = "number of players"
    private const val BOARD = "board"

    init {
        category = FarmersCategory
        name = "leader-board"
        aliases = arrayOf("lb")
        help = "Shows the Earnings Bonus leader board"
        parameters = listOf(
            FlaggedOption(TOP)
                .setShortFlag('t')
                .setLongFlag("top")
                .setStringParser(INTEGER_PARSER)
                .setHelp("Show only the top `<$TOP>`."),
            FlaggedOption(BOARD)
                .setShortFlag('b')
                .setLongFlag("board")
                .setDefault("earnings-bonus")
                .setStringParser(
                    EnumeratedStringParser.getParser(
                        values().joinToString(";") { board ->
                            "${board.longForm};${board.shortForm}"
                        },
                        false,
                        false
                    )
                )
                .setHelp("Show the specified `<$BOARD>`. The available boards are:\n" +
                        values().joinToString("\n") { board -> "    • `${board.longForm}` or `${board.shortForm}`" }),
            compactSwitch,
            extendedSwitch
        )
        sendTyping = true
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val farmers = transaction {
            Farmer.all().toList().sortedByDescending { it.earningsBonus }
        }

        if (farmers.isEmpty()) return event.replyAndLogWarning("There are no registered farmers.")

        val top = parameters.getIntOrNull(TOP)

        if (top != null && top < 1) return event.replyAndLogWarning("${TOP.capitalize()} must be a positive number.")

        val board = parameters.getStringOrNull(BOARD)?.let { input ->
            Board.getByString(input)
        }

        formatLeaderBoard(
            farmers = farmers,
            board = board ?: EARNINGS_BONUS,
            top = top,
            compact = parameters.getBoolean(COMPACT),
            extended = parameters.getBoolean(EXTENDED)
        ).let { messages ->
            if (event.channel == botCommandsChannel) {
                messages.forEach { message -> event.reply(message) }
            } else {
                event.replyInDms(messages)
            }
        }
    }

    enum class Board {
        EARNINGS_BONUS, SOUL_EGGS, PROPHECY_EGGS, PRESTIGES, DRONE_TAKEDOWNS, ELITE_DRONE_TAKEDOWNS;

        companion object {
            fun getByString(input: String): Board? =
                getByLongForm(input) ?: getByShortForm(input)

            private fun getByLongForm(input: String): Board? =
                values().find { board -> board.longForm == input }

            private fun getByShortForm(input: String): Board? =
                values().find { board -> board.shortForm == input }
        }

        val longForm: String get() = name.toLowerCase().replace('_', '-')
        val shortForm: String get() = name.toLowerCase().split('_').joinToString("") { "${it.first()}" }
    }

    fun formatLeaderBoard(
        farmers: List<Farmer>,
        board: Board,
        top: Int?,
        compact: Boolean,
        extended: Boolean,
    ): List<String> = table {
        val sortedFarmers = when (board) {
            EARNINGS_BONUS -> farmers.sortedByDescending { farmer -> farmer.earningsBonus }
            SOUL_EGGS -> farmers.sortedByDescending { farmer -> farmer.soulEggs }
            PROPHECY_EGGS -> farmers.sortedByDescending { farmer -> farmer.prophecyEggs }
            PRESTIGES -> farmers.sortedByDescending { farmer -> farmer.prestiges }
            DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.droneTakedowns }
            ELITE_DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.eliteDroneTakedowns }
        }.let { sortedFarmers ->
            if (top != null) sortedFarmers.take(top) else sortedFarmers
        }
        val shortenedNames = sortedFarmers.map { farmer ->
            farmer.inGameName.let { name ->
                if (name.length <= 10) name
                else "${name.substring(0 until 9)}…"
            }
        }

        title = "__**${
            when (board) {
                EARNINGS_BONUS -> "💵 Earnings Bonus"
                SOUL_EGGS -> "${emoteSoulEgg?.asMention ?: "🥚"} Soul Eggs"
                PROPHECY_EGGS -> "${emoteProphecyEgg?.asMention ?: "🥚"} Prophecy Eggs"
                PRESTIGES -> "🥨 Prestiges"
                DRONE_TAKEDOWNS -> "✈🚫 Drone Takedowns"
                ELITE_DRONE_TAKEDOWNS -> "🎖✈🚫 Elite Drone Takedowns"
            }
        }${if (!compact) " Leader Board" else ""}**__"
        displayHeader = true
        if (compact) incrementColumn() else incrementColumn(":")
        column {
            header = "Name"
            leftPadding = 1
            rightPadding = if (compact) 1 else 2
            cells = if (compact) shortenedNames else sortedFarmers.map { farmer -> farmer.inGameName }
        }
        column {
            header = when (board) {
                EARNINGS_BONUS -> "Earnings Bonus" + if (compact) "" else "  " // Added spacing for percent suffix
                SOUL_EGGS -> "Soul Eggs"
                PROPHECY_EGGS -> "Prophecy Eggs"
                PRESTIGES -> "Prestiges"
                DRONE_TAKEDOWNS -> "Drone Takedowns"
                ELITE_DRONE_TAKEDOWNS -> "Elite Drone Takedowns"
            }
            alignment = RIGHT
            cells = when (board) {
                EARNINGS_BONUS -> sortedFarmers.map { farmer ->
                    if (extended) farmer.earningsBonus.formatInteger() + "\u00A0%"
                    else farmer.earningsBonus.asIllions(shortened = true) + if (compact) "" else "\u00A0%"
                }
                SOUL_EGGS -> sortedFarmers.map { farmer ->
                    if (extended) farmer.soulEggs.formatInteger()
                    else farmer.soulEggs.asIllions(shortened = compact)
                }
                PROPHECY_EGGS -> sortedFarmers.map { farmer -> farmer.prophecyEggs.formatInteger() }
                PRESTIGES -> sortedFarmers.map { farmer -> farmer.prestiges.formatInteger() }
                DRONE_TAKEDOWNS -> sortedFarmers.map { farmer -> farmer.droneTakedowns.formatInteger() }
                ELITE_DRONE_TAKEDOWNS -> sortedFarmers.map { farmer -> farmer.eliteDroneTakedowns.formatInteger() }
            }
        }
        if (board == EARNINGS_BONUS) column {
            header = if (compact) "Role" else "Farmer Role"
            leftPadding = if (compact) 1 else 2
            cells = sortedFarmers.map { farmer -> farmer.earningsBonus.asRank(shortened = compact) }
        }
    }
}
