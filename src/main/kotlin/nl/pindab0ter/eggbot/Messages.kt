package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.simulation.ContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.Duration
import java.math.RoundingMode.HALF_UP

object Messages {
    private data class NameToValue(val name: String, val value: String)

    private fun leaderBoard(title: String, farmers: List<NameToValue>): List<String> =
        StringBuilder("$title leader board:\n").apply {
            append("```")
            farmers.forEachIndexed { index, (name, value) ->
                appendPaddingCharacters(index + 1, farmers.count())
                append("${index + 1}:")
                append(" ")
                append(name)
                appendPaddingCharacters(name, farmers.map { it.name })
                append("  ")
                appendPaddingCharacters(
                    value.split(Regex("[^,.\\d]"), 2).first(),
                    farmers.map { it.value.split(Regex("[^,.\\d]"), 2).first() })
                append(value)
                if (index < farmers.size - 1) appendln()
            }
        }.toString().splitMessage(prefix = "```", postfix = "```")

    fun earningsBonusLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Earnings Bonus",
        farmers.map { NameToValue(it.inGameName, it.earningsBonus.formatInteger() + "\u00A0%") }
    )

    fun earningsBonusLeaderBoardCompact(farmers: List<Farmer>): List<String> = leaderBoard(
        "Earnings Bonus",
        farmers.map { NameToValue(it.inGameName, it.earningsBonus.formatIllions() + "\u00A0%") }
    )

    fun soulEggsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Soul Eggs",
        farmers.map { NameToValue(it.inGameName, it.soulEggs.formatInteger()) }
    )

    fun prestigesLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Prestiges",
        farmers.map { NameToValue(it.inGameName, it.prestiges.formatInteger()) }
    )

    fun droneTakedownsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.droneTakedowns.formatInteger()) }
    )

    fun eliteDroneTakedownsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Elite Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.eliteDroneTakedowns.formatInteger()) }
    )

    fun earningsBonus(
        farmer: Farmer,
        compact: Boolean = false
    ): String = StringBuilder().apply {
        data class Line(
            val label: String,
            val value: String,
            var padding: String = "",
            val suffix: String? = null
        )

        val roleLabel = "Role:  "
        val role = farmer.role?.name ?: "Unknown"
        val earningsBonusLabel = "Earnings bonus:  "
        val earningsBonus = farmer.earningsBonus
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val earningsBonusSuffix = " %"
        val soulEggsLabel = "Soul Eggs:  "
        val soulEggs = farmer.soulEggs
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val soulEggsSuffix = " SE"
        val prophecyEggsLabel = "Prophecy Eggs:  "
        val prophecyEggs = farmer.prophecyEggs.formatInteger()
        val prophecyEggsSuffix = " PE"
        val soulBonusLabel = "Soul Food:  "
        val soulBonus = "${farmer.soulBonus.formatInteger()}/140"
        val prophecyBonusLabel = "Prophecy Bonus:  "
        val prophecyBonus = "${farmer.prophecyBonus.formatInteger()}/5"
        val soulEggsToNextLabel = "SE to next rank:  "
        val soulEggsToNext = farmer.nextRole
            ?.lowerBound
            ?.minus(farmer.earningsBonus)
            ?.divide(farmer.bonusPerSoulEgg, HALF_UP)
            ?.let { (if (compact) it.formatIllions() else it.formatInteger()) }
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
        }.plus(Line(soulEggsToNextLabel, soulEggsToNext, suffix = soulEggsSuffix))

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

    }.toString()

    // TODO: Display which bottlenecks will be reached when
    fun soloStatus(
        simulation: ContractSimulation,
        compact: Boolean = false
    ): String = StringBuilder().apply {
        val eggEmote = Config.eggEmojiIds[simulation.egg]?.let { id ->
            EggBot.jdaClient.getEmoteById(id)?.asMention
        } ?: ""

        // region Basic info and totals

        appendln("`${simulation.farmerName}` vs. __${simulation.contractName}__: ${if (eggEmote.isBlank()) "" else " $eggEmote"}")
        appendln("**Time remaining**: ${simulation.timeRemaining.asDaysHoursAndMinutes(compact)}")
        append("**Current chickens**: ${simulation.currentPopulation.formatIllions()} ")
        append("(${simulation.populationIncreasePerHour.formatIllions()}/hr)")
        appendln()
        append("**Current eggs**: ${simulation.currentEggs.formatIllions()} ")
        append("(${(simulation.eggsPerChickenPerMinute * simulation.currentPopulation * 60).formatIllions()}/hr)")
        appendln()
        appendln("**Eggspected**: ${simulation.eggspected.formatIllions()}")
        appendln()

        // endregion Basic info and totals

        // region Goals

        if (simulation.finished) {
            appendln("**You have successfully finished this contract! ${Config.emojiSuccess}**")
        } else {
            append("Goals (${simulation.goalReachedMoments.count { it.moment != null }}/${simulation.goals.count()}):")
            if (!compact) append("  _(Includes new chickens and takes bottlenecks into account)_")
            append("```")
            appendln()

            simulation.goalReachedMoments
                .filter { it.moment != Duration.ZERO }
                .forEachIndexed { index, (goal, moment) ->
                    val success = moment != null && moment < simulation.timeRemaining

                    append("${index + 1}: ")
                    appendPaddingCharacters(
                        goal.formatIllions(true),
                        simulation.goalReachedMoments
                            .filter { simulation.projectedEggs < it.target }
                            .map { it.target.formatIllions(true) }
                    )
                    append(goal.formatIllions(true))
                    append(if (success) " ✓ " else " ✗ ")
                    when (moment) {
                        null -> append("More than a year")
                        else -> append(moment.asDaysHoursAndMinutes(compact))
                    }
                    if (index + 1 < simulation.goals.count()) appendln()
                }
            appendln("```")
        }

        // endregion Goals

    }.toString()

    fun coopStatus(result: CoopContractSimulationResult, compact: Boolean = false): List<String> = when (result) {
        is InProgress -> coopInProgress(result.simulation, compact)
        is NotFound -> listOf(
            "No co-op found for contract `${result.contractId}` with name `${result.coopId}"
        )
        is Abandoned -> listOf(
            """ `${result.coopStatus.coopId}` vs. __${result.contractName}__:
                
                This co-op has no members.""".trimIndent()
        )
        is Failed -> listOf(
            """ `${result.coopStatus.coopId}` vs. __${result.contractName}__:
                
                This co-op has not reached their final goal.""".trimIndent()
        )
        is Finished -> listOf(
            """ `${result.coopStatus.coopId}` vs. __${result.contractName}__:
                
                This co-op has successfully finished their contract! ${Config.emojiSuccess}""".trimIndent()
        )
    }

    private fun coopInProgress(
        simulation: CoopContractSimulation,
        compact: Boolean
    ): List<String> = StringBuilder().apply {
        val eggEmote = Config.eggEmojiIds[simulation.egg]?.let { id ->
            EggBot.jdaClient.getEmoteById(id)?.asMention
        } ?: ""

        val farms = simulation.farms


        // region Basic info and totals

        appendln("`${simulation.coopId}` vs. __${simulation.contractName}__: ${if (eggEmote.isBlank()) "" else " $eggEmote"}")
        appendln("**Time remaining**: ${simulation.timeRemaining.asDaysHoursAndMinutes(compact)}")
        append("**Current chickens**: ${simulation.currentPopulation.formatIllions()} ")
        append("(${simulation.populationIncreaseRatePerHour.formatIllions()}/hr)")
        appendln()
        append("**Current eggs**: ${simulation.currentEggs.formatIllions()} ")
        append("(${simulation.currentEggsPerHour.formatIllions()}/hr)")
        appendln()
        appendln("**Eggspected**: ${simulation.eggspected.formatIllions()}")
        appendln()

        // endregion Basic info and totals

        // region Goals

        append("Goals (${simulation.goals.count { simulation.currentEggs >= it }}/${simulation.goals.count()}):")
        if (!compact) append("  _(Includes new chickens and takes bottlenecks into account)_")
        append("```")
        appendln()

        simulation.goalReachedMoments.forEachIndexed { index, (target, moment) ->
            // .filter { it.moment != Duration.ZERO }
            if (moment == Duration.ZERO) return@forEachIndexed

            append("${index + 1}: ")
            appendPaddingCharacters(
                target.formatIllions(true),
                simulation.goals
                    .filter { simulation.currentEggs < target }
                    .map { target.formatIllions(true) }
            )
            append(target.formatIllions(true))
            appendPaddingCharacters(
                target.formatIllions(true),
                simulation.goalReachedMoments.map { it.target.formatIllions(true) },
                " "
            )
            append(if (moment != null && moment < simulation.timeRemaining) " ✓ " else " ✗ ")
            if (moment == null) append("More than a year")
            else append(moment.asDaysHoursAndMinutes(compact))
            if (index + 1 < simulation.goals.count()) appendln()
        }
        appendln("```")

        // endregion Goals

        // region Members

        appendln("Members (${farms.count()}/${simulation.maxCoopSize}):")
        appendln("```")

        val name = "Name"
        val eggs = "Eggs"
        val eggRate = "Egg Rate"
        val chickens = "Chickens"
        val chickenRate = "Chicken Rate"

        // region Table header

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

        farms.forEachIndexed { index, farm ->
            appendPaddingCharacters(index + 1, farms.count())
            append("${index + 1}: ")
            when (compact) {
                true -> {
                    append("${farm.farmerName.substring(0..4)}… ")
                    appendPaddingCharacters(
                        "${farm.farmerName.substring(0..4)}… ${if (!farm.isActive) "  zZ" else " "}",
                        farms.map { "${farm.farmerName.substring(0..4)}… ${if (!farm.isActive) "  zZ" else " "}" }
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

    }.toString().splitMessage(prefix = "```", postfix = "```")
}
