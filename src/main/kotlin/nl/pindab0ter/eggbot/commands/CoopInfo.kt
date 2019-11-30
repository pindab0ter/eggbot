package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.ContractsCategory
import nl.pindab0ter.eggbot.network.AuxBrain.getCoopStatus
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.Duration

@Suppress("FoldInitializerAndIfToElvis")
object CoopInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop"
        aliases = arrayOf("coopinfo", "ci", "coop-info", "co-op", "co-op-info")
        arguments = "<contract id> <co-op id> [compact]"
        help = "Shows the progress of a specific co-op."
        category = ContractsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            minArguments = 2,
            maxArguments = 3
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractId: String = event.arguments[0]
        val coopId: String = event.arguments[1]
        val compact: Boolean = event.arguments.getOrNull(2)?.startsWith("c") == true

        getCoopStatus(contractId, coopId).let getCoopStatus@{ (status, _) ->
            if (status == null || !status.isInitialized) "Could not get co-op status. Are the `contract id` and `co-op id` correct?.".let {
                event.replyWarning(it)
                log.debug { it }
                return@getCoopStatus
            }

            message(
                CoopContractSimulation.Factory(status.contractId, status.coopId),
                compact
            ).let { messages ->
                if (event.channel.id == Config.botCommandsChannel) {
                    messages.forEach { message -> event.reply(message) }
                } else {
                    event.replyInDms(messages)
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
            }
        }
    }

    fun message(result: CoopContractSimulationResult, compact: Boolean = false): List<String> = when (result) {
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
        is CoopContractSimulationResult.InProgress -> result.simulation.let { simulation ->
            StringBuilder().apply {
                val eggEmote = Config.eggEmojiIds[simulation.egg]?.let { id ->
                    EggBot.jdaClient.getEmoteById(id)?.asMention
                } ?: "🥚"
                val farms = simulation.farms

                appendln("`${simulation.coopId}` vs. _${simulation.contractName}_:")
                appendln()

                // region Goals

                appendln("__$eggEmote **Goals** (${simulation.goalsReached}/${simulation.goals.count()}):__ ```")
                simulation.goalReachedMoments.forEachIndexed { index, (goal, moment) ->
                    append("${index + 1}. ")
                    appendPaddingCharacters(
                        goal.formatIllions(true),
                        simulation.goalReachedMoments.map { it.target.formatIllions(rounded = true) }
                    )
                    append(goal.formatIllions(true))
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

                appendln("__🗒️ **Basic info**:__ ```")
                simulation.apply {
                    appendln("Eggspected:       ${eggspected.formatIllions()}")
                    appendln("Time remaining:   ${timeRemaining.asDaysHoursAndMinutes(compact)}")
                    append("Current chickens: ${currentPopulation.formatIllions()} ")
                    if (!compact) append("(${populationIncreasePerHour.formatIllions()}/hr)")
                    appendln()
                    append("Current eggs:     ${currentEggs.formatIllions()} ")
                    if (!compact) append("(${eggsPerHour.formatIllions()}/hr) ")
                    appendln()
                    appendln("```")
                }

                // endregion Basic info and totals

                // region Members

                appendln("__🚜 **Members** (${farms.count()}/${simulation.maxCoopSize}):__")
                appendln("```")

                // region Table header

                val name = "Name"
                val eggs = "Eggs"
                val eggRate = "Egg Rate"
                val chickens = "Chickens"
                val chickenRate = "Chicken Rate"

                if (!compact) {
                    appendPaddingCharacters("", farms.count(), "#")
                    append(": $name ")
                    appendPaddingCharacters(name, farms.map { it.farmerName + if (!it.isActive) "  zZ" else " " })
                    appendPaddingCharacters(eggs, farms.map { it.currentEggs.formatIllions() })
                    append(eggs)
                    append("│")
                    append(eggRate)
                    appendPaddingCharacters(
                        eggRate,
                        farms.map { it.currentEggsPerHour.formatIllions() + "/hr" }.plus(eggRate)
                    )
                    append("│")
                    appendPaddingCharacters(
                        chickens,
                        farms.map { it.currentPopulation.formatIllions() }.plus(chickens)
                    )
                    append(chickens)
                    append("|$chickenRate")
                    appendln()

                    appendPaddingCharacters("", farms.count(), "═")
                    append("═══")
                    appendPaddingCharacters("", farms.map { it.farmerName + if (!it.isActive) "  zZ" else " " }, "═")
                    appendPaddingCharacters("", farms.map { it.currentEggs.formatIllions() }, "═")
                    append("╪")
                    appendPaddingCharacters(
                        "",
                        farms.map { "${it.currentEggsPerHour.formatIllions()}/hr" }.plus(eggRate),
                        "═"
                    )
                    append("╪")
                    appendPaddingCharacters(
                        "",
                        farms.map { it.currentPopulation.formatIllions() }.plus(chickens),
                        "═"
                    )
                    append("╪")
                    appendPaddingCharacters(
                        "",
                        farms.map { "${it.populationIncreasePerHour.formatIllions()}/hr" }.plus(chickenRate),
                        "═"
                    )
                    appendln()
                }

                // endregion Table header

                // region Table body

                val shortenedNames = farms.map { farm ->
                    farm.farmerName.let { name ->
                        if (name.length <= 9) name
                        else "${name.substring(0 until 9)}…"
                    }
                }

                farms.forEachIndexed { index, farm ->
                    appendPaddingCharacters(index + 1, farms.count())
                    append("${index + 1}: ")
                    when (compact) {
                        true -> {
                            append("${shortenedNames[index]} ")
                            appendPaddingCharacters(
                                "${shortenedNames[index]} ${if (!farm.isActive) "  zZ" else " "}",
                                farms.mapIndexed { i, f -> "${shortenedNames[i]} ${if (!f.isActive) "  zZ" else " "}" }
                            )
                        }
                        false -> {
                            append(farm.farmerName)
                            appendPaddingCharacters(
                                farm.farmerName + if (!farm.isActive) "  zZ" else " ",
                                farms.map { it.farmerName + if (!it.isActive) "  zZ" else " " }.plus(name)
                            )
                            if (!farm.isActive) append("  zZ ")
                            else append("  ")
                        }
                    }
                    appendPaddingCharacters(
                        farm.currentEggs.formatIllions(),
                        farms.map { it.currentEggs.formatIllions() }.plus(eggs)
                    )
                    append(farm.currentEggs.formatIllions())
                    append("│")
                    append("${farm.currentEggsPerHour.formatIllions()}/hr")
                    if (!compact) appendPaddingCharacters(
                        "${farm.currentEggsPerHour.formatIllions()}/hr",
                        farms.map { "${it.currentEggsPerHour.formatIllions()}/hr" }.plus(eggRate)
                    )
                    if (!compact) {
                        append("│")
                        appendPaddingCharacters(
                            farm.currentPopulation.formatIllions(),
                            farms.map { it.currentPopulation.formatIllions() }.plus(chickens)
                        )
                        append(farm.currentPopulation.formatIllions())
                        append("│")
                        append("${farm.populationIncreasePerHour.formatIllions()}/hr")
                    }
                    appendln()
                }

                // endregion Table body

                // endregion Members

                // region Bottlenecks

                simulation.farms
                    .filter { it.habBottleneckReached != null || it.transportBottleneckReached != null }
                    .let BottleneckedFarms@{ bottleneckedFarms ->
                        if (bottleneckedFarms.isEmpty()) return@BottleneckedFarms
                        appendln("```")
                        appendln("__⚠ **Bottlenecks**__: ```")
                        bottleneckedFarms.forEachIndexed { i, farm ->
                            if (compact) {
                                append("${shortenedNames[i]}: ")
                                appendPaddingCharacters(shortenedNames[i], shortenedNames)
                            } else {
                                append("${farm.farmerName}: ")
                                appendPaddingCharacters(farm.farmerName, bottleneckedFarms.map { it.farmerName })
                            }

                            farm.habBottleneckReached?.let {
                                if (it == Duration.ZERO) append("🏠Full! ")
                                else append("🏠${it.asDaysHoursAndMinutes(true)} ")
                            }

                            farm.transportBottleneckReached?.let {
                                if (it == Duration.ZERO) append("🚛Full! ")
                                else append("🚛${it.asDaysHoursAndMinutes(true)} ")
                            }

                            appendln()
                        }
                    }

                // endregion Bottlenecks

            }.toString().splitMessage(prefix = "```", postfix = "```")
        }
    }
}

