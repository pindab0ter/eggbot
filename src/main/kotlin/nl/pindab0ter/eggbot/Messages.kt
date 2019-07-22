package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.commands.EarningsBonus
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.jda.commandClient
import nl.pindab0ter.eggbot.simulation.ContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulation
import nl.pindab0ter.eggbot.simulation.CoopContractSimulationResult
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
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
        target: BigDecimal? = null,
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
        val earningsBonusBuggedLabel = "Last known EB:  "
        val earningsBonus = farmer.earningsBonus
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val earningsBonusSuffix = " %"
        val soulEggsLabel = "Soul Eggs:  "
        val soulEggsBuggedLabel = "Last known SE count:  "
        val soulEggs = BigDecimal(farmer.soulEggs)
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
        val prestigesLabel = "Current prestiges:  "
        val prestiges = farmer.prestiges.formatInteger()
        val prestigesSuffix = " \uD83E\uDD68"
        val thresholdLabel = "Bug threshold:  "
        val threshold = "~ ${calculateSoulEggsFor(farmer.prestiges)
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }}"
        val soulEggsToThresholdLabel = "SE till bug:  "
        val soulEggsToThreshold = "⨦ ${(calculateSoulEggsFor(farmer.prestiges) - BigDecimal(farmer.soulEggs))
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }}"
        val yourTargetLabel = "Your target:  "
        val yourTarget = target?.let { if (compact) it.formatIllions() else it.formatInteger() }
        val requiredPrestigesLabel = "Prestiges required:  "
        val requiredPrestiges = target?.let { "${calculatePrestigesFor(it) - farmer.prestiges}" }

        append("Earnings bonus for **${farmer.inGameName}**:```\n")

        val labelsToValues: List<Line> = when {
            // Backup bug entries
            farmer.hasBackupBug -> {
                listOf(
                    Line(earningsBonusBuggedLabel, earningsBonus, suffix = earningsBonusSuffix),
                    Line(soulEggsBuggedLabel, soulEggs, suffix = soulEggsSuffix),
                    Line(prestigesLabel, prestiges, suffix = prestigesSuffix),
                    Line(thresholdLabel, threshold, suffix = soulEggsSuffix)
                ).run {
                    if (target != null) {
                        this.plus(Line(yourTargetLabel, yourTarget!!, suffix = soulEggsSuffix))
                            .plus(Line(requiredPrestigesLabel, requiredPrestiges!!, suffix = prestigesSuffix))
                    } else this
                }
            }

            // Non-backup bug entries
            else -> {
                listOf(
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
                    listOf(
                        Line(soulEggsToNextLabel, soulEggsToNext, suffix = soulEggsSuffix),
                        Line(prestigesLabel, prestiges, suffix = prestigesSuffix),
                        Line(thresholdLabel, threshold, suffix = soulEggsSuffix),
                        Line(soulEggsToThresholdLabel, soulEggsToThreshold, suffix = soulEggsSuffix)
                    )
                )
            }
        }

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

        if (farmer.hasBackupBug) {
            val spacing = if (compact) "" else String(CharArray(lines
                .first()
                .let { (x, y, z) -> x.length + y.length + z.length }
                .minus(27)
                .div(2)) { ' ' })
            appendln("$spacing┏━━━━━━━━━━━━━━━━━━━━━━━━━┓")
            appendln("$spacing┃ ‼︎ Backup bug detected ‼︎ ┃")
            appendln("$spacing┗━━━━━━━━━━━━━━━━━━━━━━━━━┛")
            appendln()
        }

        lines.forEach { (label, value, padding, suffix) ->
            if (label == prestigesLabel) appendln()
            appendln(label + padding + value + (suffix ?: ""))
        }

        appendln("```")

        if (farmer.hasBackupBug && target == null) {
            append("To see how many prestiges you need to ")
            if (compact) appendln()
            appendln("get out of the backup bug, add your target:")
            appendln("`${commandClient.textualPrefix}${EarningsBonus.name} ${EarningsBonus.arguments}`")
        }
    }.toString()

    fun soloStatus(
        simulation: ContractSimulation,
        compact: Boolean = false
    ): String = StringBuilder().apply {
        val eggEmote = Config.eggEmojiIds[simulation.egg]?.let { id ->
            EggBot.jdaClient.getEmoteById(id)?.asMention
        } ?: ""

        //
        // Basic info and totals
        //

        appendln("`${simulation.farmerName}` vs. __${simulation.contractName}__: ${if (eggEmote.isBlank()) "" else " $eggEmote"}")
        appendln("**Time remaining**: ${simulation.timeRemaining.asDayHoursAndMinutes(compact)}")
        append("**Total eggs**: ${simulation.eggsLaid.formatIllions()} ")
        append("(${simulation.eggLayingRatePerHour.formatIllions()}/hr)")
        appendln()
        append("**Total chickens**: ${simulation.population.formatIllions()} ")
        append("(${simulation.populationIncreaseRatePerHour.formatIllions()}/hr)")
        appendln()
        appendln()

        //
        // Goals
        //

        if (simulation.finished) {
            appendln("**You have successfully finished this contract! ${Config.emojiSuccess}**")
        } else {
            append("Goals (${simulation.goals.count { simulation.eggsLaid >= it.value }}/${simulation.goals.count()}):")
            if (!compact) append("  _(Includes new chickens and assumes no bottlenecks)_")
            append("```")
            appendln()
            simulation.goals
                .filter { (_, goal) -> simulation.eggsLaid < goal }
                .forEach { (index, goal: BigDecimal) ->
                    val finishedIn = simulation.projectedTimeTo(goal)
                    val success = finishedIn != null && finishedIn < simulation.timeRemaining
                    val oneYear = Duration(DateTime.now(), DateTime.now().plusYears(1))

                    append("${index + 1}: ")
                    appendPaddingCharacters(
                        goal.formatIllions(true),
                        simulation.goals
                            .filter { simulation.eggsLaid < it.value }
                            .map { it.value.formatIllions(true) }
                    )
                    append(goal.formatIllions(true))
                    append(if (success) " ✓ " else " ✗ ")
                    when {
                        finishedIn == null -> append("∞")
                        finishedIn > oneYear -> append("More than a year")
                        else -> append(finishedIn.asDayHoursAndMinutes(compact))
                    }
                    if (index + 1 < simulation.goals.count()) appendln()
                }
            appendln("```")
        }
    }.toString()

    fun coopStatus(result: CoopContractSimulationResult, compact: Boolean = false): List<String> = when (result) {
        is CoopContractSimulationResult.InProgress -> coopInProgress(result.simulation, compact)
        is CoopContractSimulationResult.NotFound -> listOf(
            "No co-op found for contract `${result.contractId}` with name `${result.coopId}"
        )
        is CoopContractSimulationResult.Empty -> listOf(
            """ `${result.coopStatus.coopIdentifier}` vs. __${result.contractName}__:
                
                This co-op has no members.""".trimIndent()
        )
        is CoopContractSimulationResult.Finished -> listOf(
            """ `${result.coopStatus.coopIdentifier}` vs. __${result.contractName}__:
                
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

        //
        // Basic info and totals
        //

        appendln("`${simulation.coopId}` vs. __${simulation.contractName}__: ${if (eggEmote.isBlank()) "" else " $eggEmote"}")
        appendln("**Time remaining**: ${simulation.timeRemaining.asDayHoursAndMinutes(compact)}")
        append("**Total eggs**: ${simulation.eggsLaid.formatIllions()} ")
        append("(${simulation.eggLayingRatePerHour.formatIllions()}/hr)")
        appendln()
        append("**Total chickens**: ${simulation.population.formatIllions()} ")
        append("(${simulation.populationIncreaseRatePerHour.formatIllions()}/hr)")
        appendln()
        appendln()

        //
        // Goals
        //

        append("Goals (${simulation.goals.count { simulation.eggsLaid >= it.value }}/${simulation.goals.count()}):")
        if (!compact) append("  _(Includes new chickens and assumes no bottlenecks)_")
        append("```")
        appendln()
        simulation.goals
            .filter { (_, goal) -> simulation.eggsLaid < goal }
            .forEach { (index, goal: BigDecimal) ->
                val finishedIn = simulation.projectedTimeTo(goal)
                val success = finishedIn != null && finishedIn < simulation.timeRemaining
                val oneYear = Duration(DateTime.now(), DateTime.now().plusYears(1))

                appendPaddingCharacters(index + 1, farms.count())
                append("${index + 1}: ")
                appendPaddingCharacters(
                    goal.formatIllions(true),
                    simulation.goals
                        .filter { simulation.eggsLaid < it.value }
                        .map { it.value.formatIllions(true) }
                )
                append(goal.formatIllions(true))
                append(if (success) " ✓ " else " ✗ ")
                when {
                    finishedIn == null -> append("∞")
                    finishedIn > oneYear -> append("More than a year")
                    else -> append(finishedIn.asDayHoursAndMinutes(compact))
                }
                if (index + 1 < simulation.goals.count()) appendln()
            }
        appendln("```")

        //
        // Members
        //

        appendln("Members (${farms.count()}/${simulation.maxCoopSize}):")
        appendln("```")

        val name = "Name"
        val eggs = "Eggs"
        val eggRate = "Egg Rate"
        val chickens = "Chickens"
        val chickenRate = "Chicken Rate"

        // Table header
        if (!compact) {
            appendPaddingCharacters("", farms.count(), "#")
            append(": $name ")
            appendPaddingCharacters(name, farms.map { it.farmerName + if (!it.isActive) "  zZ" else " " })
            appendPaddingCharacters(eggs, farms.map { it.eggsLaid.formatIllions() })
            append(eggs)
            append("│")
            append(eggRate)
            appendPaddingCharacters(
                eggRate,
                farms.map { it.eggLayingRatePerHour.formatIllions() + "/hr" }.plus(eggRate)
            )
            append("│")
            appendPaddingCharacters(
                chickens,
                farms.map { it.population.formatIllions() }.plus(chickens)
            )
            append(chickens)
            append("|$chickenRate")
            appendln()

            appendPaddingCharacters("", farms.count(), "═")
            append("═══")
            appendPaddingCharacters("", farms.map { it.farmerName + if (!it.isActive) "  zZ" else " " }, "═")
            appendPaddingCharacters("", farms.map { it.eggsLaid.formatIllions() }, "═")
            append("╪")
            appendPaddingCharacters(
                "",
                farms.map { "${it.eggLayingRatePerHour.formatIllions()}/hr" }.plus(eggRate),
                "═"
            )
            append("╪")
            appendPaddingCharacters(
                "",
                farms.map { it.population.formatIllions() }.plus(chickens),
                "═"
            )
            append("╪")
            appendPaddingCharacters(
                "",
                farms.map { "${it.populationIncreaseRatePerHour.formatIllions()}/hr" }.plus(chickenRate),
                "═"
            )
            appendln()
        }

        // Table body
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
                farm.eggsLaid.formatIllions(),
                farms.map { it.eggsLaid.formatIllions() }.plus(eggs)
            )
            append(farm.eggsLaid.formatIllions())
            append("│")
            append("${farm.currentEggLayingRatePerHour.formatIllions()}/hr")
            if (!compact) appendPaddingCharacters(
                "${farm.currentEggLayingRatePerHour.formatIllions()}/hr",
                farms.map { "${it.currentEggLayingRatePerHour.formatIllions()}/hr" }.plus(eggRate)
            )
            if (!compact) {
                append("│")
                appendPaddingCharacters(
                    farm.population.formatIllions(),
                    farms.map { it.population.formatIllions() }.plus(chickens)
                )
                append(farm.population.formatIllions())
                append("│")
                append("${farm.populationIncreaseRatePerHour.formatIllions()}/hr")
            }
            appendln()
        }
    }.toString().splitMessage(prefix = "```", postfix = "```")
}
