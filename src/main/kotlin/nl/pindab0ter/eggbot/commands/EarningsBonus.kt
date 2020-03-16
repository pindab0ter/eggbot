package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
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
import nl.pindab0ter.eggbot.utilities.Table.AlignedColumn.Alignment.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode

object EarningsBonus : EggBotCommand() {

    private val log = KotlinLogging.logger { }
    private const val EXTENDED = "extended"

    // TODO: Make non-compact by default (https://dynalist.io/d/UURDu2p-Y76LCBzfbxg7qhlI#z=Zu2QXVgzkeKlaQ4viifuDA_-)

    init {
        category = FarmersCategory
        name = "earnings-bonus"
        aliases = arrayOf("eb")
        help = "Shows your EB, EB rank and how much SE till your next rank."
        parameters = listOf(
            Switch(EXTENDED)
                .setShortFlag('e')
                .setLongFlag("extended")
                .setHelp("Show unshortened output.")
        )
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
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

                data class Row(val label: String, val value: String, val suffix: String = "")

                val rows = mutableListOf<Row>().apply {
                    add(Row("Role:", farmer.earningsBonus.asFarmerRole()))
                    add(Row("Earnings Bonus:", farmer.earningsBonus.asIllions(shortened = true), "%"))
                    add(Row("Soul Eggs:", farmer.soulEggs.asIllions(shortened = true), "SE"))
                    add(Row("Prophecy Eggs:", farmer.prophecyEggs.formatInteger(), "PE"))
                    if (farmer.soulBonus < 140)
                        add(Row("Soul Bonus:", farmer.soulBonus.formatInteger() + "/140"))
                    if (farmer.prophecyBonus < 5)
                        add(Row("Prophecy Bonus:", farmer.prophecyBonus.formatInteger() + "/5"))
                    add(Row("Prestiges:", farmer.prestiges.formatInteger()))
                    add(Row("To next rank:", nextPowerOfThousand(farmer.earningsBonus)
                        .minus(farmer.earningsBonus)
                        .divide(farmer.bonusPerSoulEgg, RoundingMode.HALF_UP)
                        ?.asIllions(shortened = true)
                        ?.let { "+ $it" } ?: "Unknown", "SE")
                    )
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
                        leftPadding = 1
                        cells = rows.map { row -> row.suffix }
                    }
                }.let {
                    if (event.channel == botCommandsChannel) {
                        event.reply(it)
                    } else event.replyInDm(it) {
                        if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                    }
                }
            }
        }
    }
}
