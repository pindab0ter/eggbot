package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.database.Farmer
import java.math.BigInteger

object Messages {
    private data class NameToValue(val name: String, val value: String)

    private fun leaderBoard(title: String, farmers: List<NameToValue>): List<String> =
        StringBuilder("$title leader board:\n").apply {
            append("```")
            farmers.forEachIndexed { index, (name, value) ->
                append("${index + 1}:")
                appendPaddingSpaces(index + 1, farmers.count())
                append(" ")
                append(name)
                appendPaddingSpaces(name, farmers.map { it.name })
                append(" ")
                appendPaddingSpaces(
                    value,
                    farmers.map { it.value })
                append(value)
                if (index < farmers.size - 1) appendln()
            }
        }.toString().splitMessage(prefix = "Leader board continued…\n```", postfix = "```")

    fun earningsBonusLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Earnings Bonus",
        farmers.map { NameToValue(it.inGameName, it.earningsBonus.formatForDisplay() + " %") }
    )

    fun soulEggsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Soul Eggs",
        farmers.map { NameToValue(it.inGameName, it.soulEggs.formatForDisplay()) }
    )

    fun prestigesLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.prestiges.formatForDisplay()) }
    )

    fun droneTakedownsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.droneTakedowns.formatForDisplay()) }
    )

    fun eliteDroneTakedownsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Elite Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.eliteDroneTakedowns.formatForDisplay()) }
    )


    fun earningsBonus(farmer: Farmer): String = StringBuilder().apply {
        val eb = farmer.earningsBonus.formatForDisplay() + " %"
        val se = BigInteger.valueOf(farmer.soulEggs).formatForDisplay()
        val seToNext =
            farmer.nextRole
                ?.lowerBound
                ?.minus(farmer.earningsBonus)
                ?.divide(farmer.bonusPerSoulEgg)
                ?.formatForDisplay() ?: "Unknown"
        val role = farmer.role?.name ?: "Unknown"
        val strings = listOf(
            eb, se, seToNext, role
        )

        append("Earnings bonus for **${farmer.inGameName}**:```\n")
        append("Role:            ")
        append(" ".repeat(strings.maxBy { it.length }?.length?.minus(role.length) ?: 0))
        append(farmer.role?.name ?: "Unknown")
        appendln()
        append("Earnings bonus:  ")
        appendPaddingSpaces(eb.dropLast(2), strings)
        append(eb)
        appendln()
        append("Soul Eggs:       ")
        appendPaddingSpaces(se, strings)
        append(se)
        appendln()
        append("SE to next rank: ")
        appendPaddingSpaces(seToNext, strings)
        append(seToNext)
        append("```")
    }.toString()
}