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
}
