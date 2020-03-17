package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.EggBot.eggsToEmotes
import nl.pindab0ter.eggbot.commands.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.ContractSimulation
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.Duration

object SoloInfo : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    init {
        category = ContractsCategory
        name = "solo"
        help = "Shows the progress of a contract you're not in a co-op for."
        parameters = listOf(
            contractIdOption,
            compactSwitch
        )
        sendTyping = true
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val farmers = transaction { DiscordUser.findById(event.author.id)?.farmers?.toList()!! }
        val contractId: String = parameters.getString(CONTRACT_ID)
        val compact: Boolean = parameters.getBoolean(COMPACT)

        for (farmer: Farmer in farmers) AuxBrain.getFarmerBackup(farmer.inGameId)?.let { backup ->
            val contract = if (backup.hasGame()) backup.contracts.contractsList.find {
                it.contract.id == contractId
            } else "No data found for `${farmer.inGameName}`.".let {
                log.warn { it }
                event.reply(it)
                return
            }

            if (contract == null)
                "No contract found with ID `$contractId` for `${farmer.inGameName}`. Try using `${event.client.textualPrefix}${ContractIDs.name}`".let {
                    log.debug { it }
                    event.reply(it)
                    return
                }
            if (contract.contract.coopAllowed && contract.coopId.isNotBlank())
                "The contract with ID `$contractId` is not a solo contract.".let {
                    log.debug { it }
                    event.reply(it)
                    return
                }
            val simulation = ContractSimulation(backup, contractId)
                ?: "You haven't started this contract yet.".let {
                    log.debug { it }
                    event.reply(it)
                    return
                }

            simulation.run()

            message(simulation, compact).let { message ->
                if (event.channel == botCommandsChannel) {
                    event.reply(message)
                } else {
                    event.replyInDm(message)
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
            }
        }
    }

    fun message(
        simulation: ContractSimulation,
        compact: Boolean = false
    ): String = StringBuilder().apply {
        val eggEmote = eggsToEmotes[simulation.egg]?.asMention ?: "🥚"

        appendln("`${simulation.farmerName}` vs. _${simulation.contractName}_:")
        appendln()

        if (simulation.finished) {
            appendln("**You have successfully finished this contract! ${Config.emojiSuccess}**")
            return@apply
        }

        // region Goals

        appendln("__$eggEmote **Goals** (${simulation.goalsReached}/${simulation.goals.count()}):__ ```")
        simulation.goalReachedMoments.forEachIndexed { index, (goal, moment) ->
            append("${index + 1}. ")
            appendPaddingCharacters(
                goal.asIllions(true),
                simulation.goalReachedMoments.map { it.target.asIllions(rounded = true) }
            )
            append(goal.asIllions(true))
            append(
                when {
                    moment == null || moment > simulation.timeRemaining -> " 🔴 "
                    moment == Duration.ZERO -> " 🏁 "
                    else -> " 🟢 "
                }
            )
            when (moment) {
                null -> append("More than a year")
                Duration.ZERO -> append("Goal reached!")
                else -> append(moment.asDaysHoursAndMinutes(compact))
            }
            if (index + 1 < simulation.goals.count()) appendln()
        }
        appendln("```")

        // endregion Goals

        // region Basic info and totals

        appendln("__🗒️ **Basic info**__ ```")
        simulation.apply {
            appendln("Eggspected:       ${eggspected.asIllions()}")
            appendln("Time remaining:   ${timeRemaining.asDaysHoursAndMinutes(compact)}")
            append("Current chickens: ${currentPopulation.asIllions()} ")
            if (!compact) append("(${populationIncreasePerHour.asIllions()}/hr)")
            appendln()
            append("Current eggs:     ${currentEggs.asIllions()} ")
            if (!compact) append("(${(eggsPerChickenPerMinute * currentPopulation * 60).asIllions()}/hr) ")
            appendln()
            appendln("Last update:      ${timeSinceLastUpdate.asDaysHoursAndMinutes(compact)} ago")
            appendln("```")
        }

        // endregion Basic info and totals

        // region Bottlenecks

        simulation.apply {
            if (habBottleneckReached != null || transportBottleneckReached != null) {
                appendln("__**⚠ Bottlenecks**__ ```")
                habBottleneckReached?.let {
                    if (it == Duration.ZERO) append("🏠Full! ")
                    else append("🏠${it.asDaysHoursAndMinutes(true)} ")
                }
                transportBottleneckReached?.let {
                    if (it == Duration.ZERO) append("🚛Full! ")
                    else append("🚛${it.asDaysHoursAndMinutes(true)} ")
                }
                appendln("```")
            }
        }

        // endregion Bottlenecks

    }.toString()
}
