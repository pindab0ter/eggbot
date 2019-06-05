package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.auxbrain.ContractSimulation
import nl.pindab0ter.eggbot.auxbrain.CoopContractSimulation
import nl.pindab0ter.eggbot.database.Farmer
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
        val eb = farmer.earningsBonus.let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val se = BigDecimal(farmer.soulEggs).let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val pe = farmer.prophecyEggs.formatInteger()
        val sb = "${farmer.soulBonus.formatInteger()}/140"
        val pb = "${farmer.prophecyBonus.formatInteger()}/5"
        val seToNext =
            farmer.nextRole
                ?.lowerBound
                ?.minus(farmer.earningsBonus)
                ?.divide(farmer.bonusPerSoulEgg, HALF_UP)
                ?.let { (if (compact) it.formatIllions() else it.formatInteger()) }
                ?.let { "+ $it" } ?: "Unknown"
        val role = farmer.role?.name ?: "Unknown"
        val strings = listOf(
            eb, se, seToNext, role
        )

        append("Earnings bonus for **${farmer.inGameName}**:```\n")
        append("Role:            ")
        append(" ".repeat(strings.maxBy { it.length }?.length?.minus(role.length) ?: 0))
        appendln(farmer.role?.name ?: "Unknown")
        append("Earnings bonus:  ")
        appendPaddingCharacters(eb, strings)
        appendln("$eb %")
        append("Soul Eggs:       ")
        appendPaddingCharacters(se, strings)
        appendln("$se SE")

        if (farmer.soulBonus < 140) {
            append("Soul Food:       ")
            appendPaddingCharacters(sb, strings)
            appendln(sb)
        }

        append("Prophecy Eggs:   ")
        appendPaddingCharacters(pe, strings)
        appendln("$pe PE")

        if (farmer.prophecyBonus < 5) {
            append("Prophecy Bonus:  ")
            appendPaddingCharacters(pb, strings)
            appendln(pb)
        }

        append("SE to next rank: ")
        appendPaddingCharacters(seToNext, strings)
        append("$seToNext SE")
        append("```")
    }.toString()

    fun soloStatus(
        simulation: ContractSimulation
    ): String = StringBuilder("`${simulation.contractId}` (${simulation.contractName}):\n").apply {
        val eggEmote = Config.eggEmojiIds[simulation.egg]?.let { id ->
            EggBot.jdaClient.getEmoteById(id)?.asMention
        } ?: ""

        appendln("**Farmer**: `${simulation.backup.name}`")
        appendln("**Eggs**: ${simulation.eggsLaid.formatIllions()}$eggEmote")
        appendln("**Rate**: ${simulation.currentEggLayingRatePerHour.formatIllions(true)}/hr")
        // appendln("**Time remaining**: ${simulation.timeRemaining.asDayHoursAndMinutes()}")
        // appendln("**Required eggs**: ${simulation.goals.map { it.value }.maxBy { it }!!.formatIllions(true)}")
        // appendln("**Projected eggs with int. hatchery calm**: ${simulation.finalTargetWithCalm.formatIllions()}")
    }.toString()

    fun coopStatus(simulation: CoopContractSimulation, compact: Boolean = false): String = StringBuilder().apply {
        val eggEmote = Config.eggEmojiIds[simulation.egg]?.let { id ->
            EggBot.jdaClient.getEmoteById(id)?.asMention
        } ?: ""

        val farms = simulation.farms

        //
        // Basic info and totals
        //

        appendln("`${simulation.coopId}` vs. __${simulation.contractName}__: ${if (eggEmote.isBlank()) "" else " $eggEmote"}")
        appendln("**Time remaining**: ${simulation.timeRemaining.asDayHoursAndMinutes(compact)}")

        if (farms.count() == 0) {
            appendln()
            appendln("\nThis co-op has no members.")
            return@apply
        }

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

        if (simulation.goals.all { goal -> simulation.eggsLaid >= goal.value }) {
            appendln("**Contract finished! ${Config.emojiSuccess}**")
            appendln()
        } else {
            append("Goals (${simulation.goals.count { simulation.eggsLaid >= it.value }}/${simulation.goals.count()}):")
            if (!compact) append("  _(Includes new chickens and assumes no bottlenecks)_")
            append("```")
            appendln()
            simulation.goals
                .filter { (_, goal) -> simulation.eggsLaid < goal }
                .forEach { (index, goal: BigDecimal) ->
                    val finishedIn = simulation.projectedTimeRequired(goal)
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
        }

        //
        // Members
        //

        appendln("Members (${farms.count()}/${simulation.maxCoopSize}):")
        appendln("```")

        val NAME = "Name"
        val EGGS = "Eggs"
        val EGG_RATE = "Egg Rate"
        val CHICKENS = "Chickens"
        val CHICKEN_RATE = "Chicken Rate"

        // Table header
        if (!compact) {
            appendPaddingCharacters("", farms.count(), "#")
            append(": $NAME ")
            appendPaddingCharacters(NAME, farms.map { it.farmerName + if (!it.isActive) "  zZ" else " " })
            appendPaddingCharacters(EGGS, farms.map { it.eggsLaid.formatIllions() })
            append(EGGS)
            append("│")
            append(EGG_RATE)
            appendPaddingCharacters(
                EGG_RATE,
                farms.map { it.eggLayingRatePerHour.formatIllions() + "/hr" }.plus(EGG_RATE)
            )
            append("│")
            appendPaddingCharacters(
                CHICKENS,
                farms.map { it.population.formatIllions() }.plus(CHICKENS)
            )
            append(CHICKENS)
            append("|$CHICKEN_RATE")
            appendln()

            append("════")
            appendPaddingCharacters("", farms.map { it.farmerName + if (!it.isActive) "  zZ" else " " }, "═")
            appendPaddingCharacters("", farms.map { it.eggsLaid.formatIllions() }, "═")
            append("╪")
            appendPaddingCharacters(
                "",
                farms.map { "${it.eggLayingRatePerHour.formatIllions()}/hr" }.plus(EGG_RATE),
                "═"
            )
            append("╪")
            appendPaddingCharacters(
                "",
                farms.map { it.population.formatIllions() }.plus(CHICKENS),
                "═"
            )
            append("╪")
            appendPaddingCharacters(
                "",
                farms.map { "${it.populationIncreaseRatePerHour.formatIllions()}/hr" }.plus(CHICKEN_RATE),
                "═"
            )
            appendln()
        }

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
                        farms.map { it.farmerName + if (!it.isActive) "  zZ" else " " }.plus(NAME)
                    )
                    if (!farm.isActive) append("  zZ ")
                    else append("  ")
                }
            }
            appendPaddingCharacters(
                farm.eggsLaid.formatIllions(),
                farms.map { it.eggsLaid.formatIllions() }.plus(EGGS)
            )
            append(farm.eggsLaid.formatIllions())
            append("│")
            append("${farm.currentEggLayingRatePerHour.formatIllions()}/hr")
            if (!compact) appendPaddingCharacters(
                "${farm.currentEggLayingRatePerHour.formatIllions()}/hr",
                farms.map { "${it.currentEggLayingRatePerHour.formatIllions()}/hr" }.plus(EGG_RATE))
            if (!compact) {
                append("│")
                appendPaddingCharacters(
                    farm.population.formatIllions(),
                    farms.map { it.population.formatIllions() }.plus(CHICKENS)
                )
                append(farm.population.formatIllions())
                append("│")
                append("${farm.populationIncreaseRatePerHour.formatIllions()}/hr")
            }
            appendln()
        }
        appendln("```")
    }.toString()
}
