package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot.toEmote
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.commands.categories.ContractsCategory
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.network.AuxBrain.getCoopStatus
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult
import nl.pindab0ter.eggbot.utilities.*
import nl.pindab0ter.eggbot.utilities.Table.AlignedColumn.Alignment.*
import org.joda.time.Duration.*

@Suppress("FoldInitializerAndIfToElvis")
object CoopInfo : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop"
        help = "Shows info on a specific co-op, displaying the current status, player contribution and runs a " +
                "simulation to estimate whether/when the goals will be reached and if people will reach their " +
                "habitat or transport bottlenecks."
        category = ContractsCategory
        parameters = listOf(
            contractIdOption,
            coopIdOption,
            compactSwitch
        )
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val coopId: String = parameters.getString(COOP_ID)
        val compact: Boolean = parameters.getBoolean(COMPACT, false)

        val message: Message = event.channel.sendMessage("Fetching contract information…").complete()

        getCoopStatus(contractId, coopId).let getCoopStatus@{ status ->
            if (status == null || !status.isInitialized) "Could not get co-op status. Are the `contract id` and `co-op id` correct?.".let {
                event.replyWarning(it)
                log.debug { it }
                return@getCoopStatus
            }

            val simulation = CoopContractSimulation.Factory(status.contractId, status.coopId, message)

            message.delete().queue()

            messageBody(simulation, compact).let { messages ->
                if (event.channel == botCommandsChannel) {
                    messages.forEach { message -> event.reply(message) }
                } else {
                    event.replyInDms(messages)
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
            }
        }
    }

    private fun messageBody(
        result: CoopContractSimulationResult,
        compact: Boolean = false
    ): List<String> = when (result) {
        is CoopContractSimulationResult.NotFound -> listOf(
            "No co-op found for contract `${result.contractId}` with name `${result.coopId}`"
        )
        is CoopContractSimulationResult.Abandoned -> listOf(
            """ `${result.coopStatus.coopId}` vs. __${result.contractName}__:
                
                This co-op has no members.""".trimIndent()
        )
        is CoopContractSimulationResult.Failed -> listOf(
            """ `${result.coopStatus.coopId}` vs. __${result.contractName}__:
                
                This co-op has not reached their final goal.""".trimIndent()
        )
        is CoopContractSimulationResult.Finished -> listOf(
            """ `${result.coopStatus.coopId}` vs. __${result.contractName}__:
                
                This co-op has successfully finished their contract! ${Config.emojiSuccess}""".trimIndent()
        )
        is CoopContractSimulationResult.InProgress -> StringBuilder().apply {
            result.simulation.apply {
                val farms = farms
                val shortenedNames = farms.map { farm ->
                    farm.farmerName.let { name ->
                        if (name.length <= 10) name
                        else "${name.substring(0 until 9)}…"
                    }
                }

                appendln("`${coopId}` vs. _${contractName}_:")
                appendln()

                // region Goals

                appendTable {
                    title = "__${egg.toEmote()} **Goals** (${goalsReached}/${goals.count()}):__"
                    displayHeader = false
                    bottomPadding = 1

                    incrementColumn(suffix = ".")
                    column {
                        leftPadding = 1
                        cells = goals.map { goal -> goal.asIllions(true) }
                    }
                    column {
                        leftPadding = 2
                        rightPadding = 2
                        cells = goalReachedMoments.map { (_, moment) ->
                            when {
                                moment == null || moment > timeRemaining -> "🔴"
                                moment == ZERO -> "🏁"
                                else -> "🟢"
                            }
                        }
                    }
                    column {
                        cells = goalReachedMoments.map { (_, moment) ->
                            when (moment) {
                                null -> "More than a year"
                                ZERO -> "Goal reached!"
                                else -> moment.asDaysHoursAndMinutes(compact)
                            }
                        }
                    }
                }

                // endregion Goals

                // region Basic info and totals

                appendln("__🗒️ **Basic info**__ ```")
                appendln("Eggspected:       ${eggspected.asIllions()}")
                appendln("Time remaining:   ${timeRemaining.asDaysHoursAndMinutes(compact)}")
                append("Current chickens: ${currentPopulation.asIllions()} ")
                if (!compact) append("(${populationIncreasePerHour.asIllions()}/hr)")
                appendln()
                append("Current eggs:     ${currentEggs.asIllions()} ")
                if (!compact) append("(${eggsPerHour.asIllions()}/hr) ")
                appendln()
                appendln("Tokens available: $tokensAvailable")
                appendln("Tokens spent:     $tokensSpent")
                if (coopStatus.public) appendln("Access:           This co-op is PUBLIC")
                appendln("```")

                append('\u200B')

                // endregion Basic info and totals

                if (!compact) {

                    // region Non-compact

                    appendTable {
                        title = "__**🚜 Members** (${farms.count()}/${maxCoopSize}):__"
                        bottomPadding = 1

                        incrementColumn(":")
                        column {
                            header = "Name"
                            leftPadding = 1
                            rightPadding = 3
                            cells = farms.map { farm ->
                                "${farm.farmerName}${if (farm.isActive) "" else " zZ"}"
                            }
                        }
                        column {
                            header = "Eggs"
                            alignment = RIGHT
                            cells = farms.map { farm -> farm.currentEggs.asIllions() }
                        }
                        divider()
                        column {
                            header = "/hr"
                            rightPadding = 3
                            cells = farms.map { farm -> farm.currentEggsPerHour.asIllions() }
                        }
                        column {
                            header = "Chickens"
                            alignment = RIGHT
                            cells = farms.map { farm -> farm.currentPopulation.asIllions() }
                        }
                        divider()
                        column {
                            header = "/hr"
                            rightPadding = 3
                            cells = farms.map { farm -> farm.populationIncreasePerHour.asIllions() }
                        }
                        column {
                            header = "Tkns"
                            alignment = RIGHT
                            cells = farms.map { farm -> if (farm.boostTokensCurrent > 0) "${farm.boostTokensCurrent}" else "" }
                        }
                        divider()
                        column {
                            header = "Spent"
                            cells = farms.map { farm -> if (farm.boostTokensSpent > 0) "${farm.boostTokensSpent}" else "" }
                        }
                    }

                    append('\u200B')

                    // endregion Non-compact

                } else {

                    // region Compact

                    appendTable {
                        title = "__**🚜 Members** (${farms.count()}/${maxCoopSize}):__"

                        column {
                            header = "Name"
                            rightPadding = 2
                            cells = farms.zip(shortenedNames).map { (farm, name) ->
                                "$name${if (farm.isActive) "" else " zZ"}"
                            }
                        }
                        column {
                            header = "Eggs"
                            alignment = RIGHT
                            cells = farms.map { farm -> farm.currentEggs.asIllions() }
                        }
                        divider()
                        column {
                            header = "/hr"
                            rightPadding = 2
                            cells = farms.map { farm -> farm.currentEggsPerHour.asIllions() }
                        }
                    }

                    append('\u200B')

                    appendTable {
                        title = "__**🎫 Tokens**__"
                        column {
                            header = "Name"
                            rightPadding = 2
                            cells = shortenedNames
                        }
                        column {
                            header = "Tokens"
                            alignment = RIGHT
                            cells = farms.map { farm -> "${farm.boostTokensCurrent}" }
                        }
                        divider()
                        column {
                            header = "Spent"
                            cells = farms.map { farm -> "${farm.boostTokensSpent}" }
                        }
                    }

                    append('\u200B')

                    // endregion Compact
                }

                val bottleneckedFarmers = farms.zip(shortenedNames).filter { (farm, _) ->
                    farm.habBottleneckReached != null || farm.transportBottleneckReached != null
                }

                if (bottleneckedFarmers.isNotEmpty()) appendTable {
                    title = "__**⚠ Bottlenecks**__"
                    displayHeader = false

                    column {
                        rightPadding = 1
                        cells = bottleneckedFarmers.map { (farm, shortenedName) ->
                            "${if (compact) shortenedName else farm.farmerName}:"
                        }
                    }

                    column {
                        cells = bottleneckedFarmers.map { (farm, _) ->
                            val habs = farm.habBottleneckReached.let { duration ->
                                when (duration) {
                                    null -> ""
                                    ZERO -> "🏠Full! "
                                    else ->
                                        if (compact) "🏠${duration.asHoursAndMinutes()}"
                                        else "🏠${duration.asDaysHoursAndMinutes(true)} "
                                }
                            }
                            val transport = farm.transportBottleneckReached.let { duration ->
                                when (duration) {
                                    null -> ""
                                    ZERO -> "🚛Full! "
                                    else ->
                                        if (compact) "🚛${duration.asHoursAndMinutes()}"
                                        else "🚛${duration.asDaysHoursAndMinutes(true)} "
                                }
                            }
                            "$habs${if (habs.isNotEmpty()) " " else ""}$transport"
                        }
                    }
                }
            }
        }.toString().splitMessage(separator = '\u200B')
    }
}
