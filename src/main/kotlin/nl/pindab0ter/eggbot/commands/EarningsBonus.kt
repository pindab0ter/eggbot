package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.*
import nl.pindab0ter.eggbot.utilities.Table.AlignedColumn.Alignment.RIGHT
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

                data class Row(val label: String = "", val value: String = "", val suffix: String = "")

                fun MutableList<Row>.addRow(label: String = "", value: String = "", suffix: String = "") =
                    add(Row(label, value, suffix))

                val rows = mutableListOf<Row>().apply {
                    addRow("Role:", farmer.earningsBonus.asFarmerRole(shortened = compact))
                    addRow(
                        "Earnings Bonus:",
                        if (extended) farmer.earningsBonus.formatInteger()
                        else farmer.earningsBonus.asIllions(shortened = compact), " %"
                    )
                    addRow(
                        "Soul Eggs:",
                        if (extended) farmer.soulEggs.formatInteger()
                        else farmer.soulEggs.asIllions(shortened = compact)
                    )
                    addRow("Prophecy Eggs:", farmer.prophecyEggs.formatInteger())
                    if (farmer.soulBonus < 140)
                        addRow("Soul Bonus:", farmer.soulBonus.formatInteger(), "/140")
                    if (farmer.prophecyBonus < 5)
                        addRow("Prophecy Bonus:", farmer.prophecyBonus.formatInteger(), "/5")
                    addRow("Prestiges:", farmer.prestiges.formatInteger())
                    addRow(
                        "SE to next rank:", "+ ${
                        if (extended) farmer.seToNextRole.formatInteger()
                        else farmer.seToNextRole.asIllions(shortened = compact)}"
                    )
                    addRow("PE to next rank:", "+ ${farmer.peToNextRole.formatInteger()}")
                }

                table {
                    title = "Earnings bonus for **${farmer.inGameName}**"
                    displayHeader = false
                    column {
                        rightPadding = 2
                        cells = rows.map { row -> row.label }
                    }
                    column {
                        alignment = RIGHT
                        cells = rows.map { row -> row.value }
                    }
                    column {
                        cells = rows.map { row -> row.suffix }
                    }
                }.let { blocks ->
                    if (event.channel == botCommandsChannel) {
                        blocks.forEach { block -> event.reply(block) }
                    } else {
                        var firstResponse = true
                        blocks.forEach { block ->
                            event.replyInDm(block) {
                                if (event.isFromType(ChannelType.TEXT) && firstResponse) {
                                    event.reactSuccess()
                                    firstResponse = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
