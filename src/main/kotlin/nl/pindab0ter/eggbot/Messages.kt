package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.database.Farmer
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


    fun earningsBonus(farmer: Farmer, compact: Boolean = false): String = StringBuilder().apply {
        val role = farmer.role?.name ?: "Unknown"
        val earningsBonus = farmer.earningsBonus
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val soulEggs = BigDecimal(farmer.soulEggs)
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val prophecyEggs = farmer.prophecyEggs.formatInteger()
        val soulBonus = "${farmer.soulBonus.formatInteger()}/140"
        val prophecyBonus = "${farmer.prophecyBonus.formatInteger()}/5"
        val prestiges = farmer.prestiges.formatInteger()
        val soulEggsToNext = farmer.nextRole
            ?.lowerBound
            ?.minus(farmer.earningsBonus)
            ?.divide(farmer.bonusPerSoulEgg, HALF_UP)
            ?.let { (if (compact) it.formatIllions() else it.formatInteger()) }
            ?.let { "+ $it" } ?: "Unknown"
        val threshold = "~ ${calculateSoulEggsFor(farmer.prestiges)
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }}"
        val soulEggsToThreshold = "⨦ ${(calculateSoulEggsFor(farmer.prestiges) - BigDecimal(farmer.soulEggs))
            .let { (if (compact) it.formatIllions() else it.formatInteger()) }}"
        val regularStrings = listOf(
            earningsBonus,
            soulEggs,
            soulEggsToNext,
            soulBonus,
            prophecyBonus,
            prophecyEggs,
            prestiges,
            role,
            threshold,
            soulEggsToThreshold
        )
        val backupBugStrings = listOf(
            soulEggs, prestiges, threshold
        )

        append("Earnings bonus for **${farmer.inGameName}**:```\n")

        if (!farmer.hasBackupBug) {
            // TODO: Use less space if role is longest in compact mode
            // label pad + value pad - length of smallest pad
            append("Role:                ")
            appendPaddingCharacters(role, regularStrings)
            appendln(role)
            append("Earnings bonus:      ")
            appendPaddingCharacters(earningsBonus, regularStrings)
            appendln("$earningsBonus %")
            append("Soul Eggs:           ")
            appendPaddingCharacters(soulEggs, regularStrings)
            appendln("$soulEggs SE")

            if (farmer.soulBonus < 140) {
                append("Soul Food:           ")
                appendPaddingCharacters(soulBonus, regularStrings)
                appendln(soulBonus)
            }

            append("Prophecy Eggs:       ")
            appendPaddingCharacters(prophecyEggs, regularStrings)
            appendln("$prophecyEggs PE")

            if (farmer.prophecyBonus < 5) {
                append("Prophecy Bonus:      ")
                appendPaddingCharacters(prophecyBonus, regularStrings)
                appendln(prophecyBonus)
            }

            append("SE to next rank:     ")
            appendPaddingCharacters(soulEggsToNext, regularStrings)
            appendln("$soulEggsToNext SE")
            appendln()

            append("Current prestiges:   ")
            appendPaddingCharacters(prestiges, regularStrings)
            appendln(prestiges)

            append("Bug threshold:       ")
            appendPaddingCharacters(threshold, regularStrings)
            appendln("$threshold SE")

            append("SE till bug:         ")
            appendPaddingCharacters(soulEggsToThreshold, regularStrings)
            appendln("$soulEggsToThreshold SE")
        } else {
            appendln(" ┏━━━━━━━━━━━━━━━━━━━━━━━━━┓")
            appendln(" ┃ ‼︎ Backup bug detected ‼︎ ┃")
            appendln(" ┗━━━━━━━━━━━━━━━━━━━━━━━━━┛")
            appendln()

            append("Last known Soul Eggs: ")
            appendPaddingCharacters(soulEggs, backupBugStrings)
            appendln("$soulEggs SE")

            append("Current prestiges:    ")
            appendPaddingCharacters(prestiges, backupBugStrings)
            appendln(prestiges)

            append("Bug threshold:        ")
            appendPaddingCharacters(threshold, backupBugStrings)
            appendln("$threshold SE")
        }
        appendln("```")
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
