package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.utilities.*
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
}
