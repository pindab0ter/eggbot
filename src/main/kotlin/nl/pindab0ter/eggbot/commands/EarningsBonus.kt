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
import nl.pindab0ter.eggbot.utilities.asIllions
import nl.pindab0ter.eggbot.utilities.formatInteger
import nl.pindab0ter.eggbot.utilities.nextPowerOfThousand
import nl.pindab0ter.eggbot.utilities.paddingCharacters
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

                StringBuilder().apply {
                    data class Line(
                        val label: String,
                        val value: String,
                        var padding: String = "",
                        val suffix: String? = null
                    )

                    val roleLabel = "Role:  "
                    val role = farmer.role
                    val earningsBonusLabel = "Earnings bonus:  "
                    val earningsBonus =
                        if (extended) farmer.earningsBonus.formatInteger()
                        else farmer.earningsBonus.asIllions(false)
                    val earningsBonusSuffix = " %"
                    val soulEggsLabel = "Soul Eggs:  "
                    val soulEggs =
                        if (extended) farmer.soulEggs.formatInteger()
                        else farmer.soulEggs.asIllions(false)
                    val soulEggsSuffix = " SE"
                    val prophecyEggsLabel = "Prophecy Eggs:  "
                    val prophecyEggs = farmer.prophecyEggs.formatInteger()
                    val prophecyEggsSuffix = " PE"
                    val soulBonusLabel = "Soul Food:  "
                    val soulBonus = "${farmer.soulBonus.formatInteger()}/140"
                    val prophecyBonusLabel = "Prophecy Bonus:  "
                    val prophecyBonus = "${farmer.prophecyBonus.formatInteger()}/5"
                    val prestigesLabel = "Prestiges: "
                    val prestiges = "${farmer.prestiges}"
                    val soulEggsToNextLabel = "SE to next rank:  "
                    val soulEggsToNext = nextPowerOfThousand(farmer.earningsBonus)
                        .minus(farmer.earningsBonus)
                        .divide(farmer.bonusPerSoulEgg, RoundingMode.HALF_UP)
                        ?.asIllions(rounded = false, shortened = true)
                        ?.let { "+ $it" } ?: "Unknown"

                    append("Earnings bonus for **${farmer.inGameName}**:```\n")

                    val labelsToValues: List<Line> = listOf(
                        Line(roleLabel, role),
                        Line(earningsBonusLabel, earningsBonus, suffix = earningsBonusSuffix),
                        Line(soulEggsLabel, soulEggs, suffix = soulEggsSuffix),
                        Line(prophecyEggsLabel, prophecyEggs, suffix = prophecyEggsSuffix)
                    ).run {
                        if (farmer.soulBonus < 140) this.plus(Line(soulBonusLabel, soulBonus))
                        else this
                    }.run {
                        if (farmer.prophecyBonus < 5) this.plus(Line(prophecyBonusLabel, prophecyBonus))
                        else this
                    }.plus(
                        arrayOf(
                            Line(prestigesLabel, prestiges),
                            Line(soulEggsToNextLabel, soulEggsToNext, suffix = soulEggsSuffix)
                        )
                    )

                    // TODO: Clean up this code

                    val lines = labelsToValues.map { (label, value, _, suffix) ->
                        val padding = paddingCharacters(label, labelsToValues.map { it.label }) +
                                paddingCharacters(value, labelsToValues.map { it.value })
                        Line(label, value, padding, suffix)
                    }.let { lines ->
                        val shortestPadding = lines.map { it.padding }.minBy { it.length }?.length ?: 0
                        lines.map { (label, value, padding, suffix) ->
                            Line(label, value, padding.drop(shortestPadding), suffix)
                        }
                    }

                    lines.forEach { (label, value, padding, suffix) ->
                        appendln(label + padding + value + (suffix ?: ""))
                    }

                    appendln("```")

                }.toString().let {
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
