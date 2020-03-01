package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.RoundingMode

object EarningsBonus : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "earnings-bonus"
        aliases = arrayOf("eb", "earning-bonus")
        help = "Shows your EB, EB rank and how much SE till your next rank"
        category = FarmersCategory
        arguments = "[extended]"
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            maxArguments = 1
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val extended = event.arguments.any { it.startsWith("e") }

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
                    val role = farmer.role?.name ?: "Unknown"
                    val earningsBonusLabel = "Earnings bonus:  "
                    val earningsBonus =
                        if (extended) farmer.earningsBonus.formatInteger()
                        else farmer.earningsBonus.formatIllions(false)
                    val earningsBonusSuffix = " %"
                    val soulEggsLabel = "Soul Eggs:  "
                    val soulEggs =
                        if (extended) farmer.soulEggs.formatInteger()
                        else farmer.soulEggs.formatIllions(false)
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
                    val soulEggsToNext = farmer.nextRole
                        ?.lowerBound
                        ?.minus(farmer.earningsBonus)
                        ?.divide(farmer.bonusPerSoulEgg, RoundingMode.HALF_UP)
                        ?.formatIllions(false)
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
                    if (event.channel == EggBot.botCommandsChannel) {
                        event.reply(it)
                    } else event.replyInDm(it) {
                        if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                    }
                }
            }
        }
    }
}
