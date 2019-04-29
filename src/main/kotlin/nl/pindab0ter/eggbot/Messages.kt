package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.database.Farmer
import java.math.BigInteger

object Messages {
    fun earningsBonusLeaderBoard(farmers: List<Farmer>): List<String> =
        StringBuilder("Earnings Bonus leader board:\n").apply {
            append("```")
            farmers.forEachIndexed { index, farmer ->
                append("${index + 1}:")
                appendPaddingSpaces(index + 1, farmers.count())
                append(" ")
                append(farmer.inGameName)
                appendPaddingSpaces(farmer.inGameName, farmers.map { it.inGameName })
                append(" ")
                appendPaddingSpaces(
                    farmer.earningsBonus.formatForDisplay() + " %",
                    farmers.map { it.earningsBonus.formatForDisplay() + " %" })
                append(farmer.earningsBonus.formatForDisplay() + " %")
                if (index < farmers.size - 1) appendln()
            }
        }.toString().splitMessage(prefix = "Leader board continued…\n```", postfix = "```")

    fun soulEggsLeaderBoard(farmers: List<Farmer>): List<String> =
        StringBuilder("Soul Eggs leader board:\n").apply {
            append("```")
            farmers.forEachIndexed { index, farmer ->
                append("${index + 1}:")
                appendPaddingSpaces(index + 1, farmers.count())
                append(" ")
                append(farmer.inGameName)
                appendPaddingSpaces(farmer.inGameName, farmers.map { it.inGameName })
                append(" ")
                appendPaddingSpaces(
                    farmer.soulEggs.formatForDisplay(),
                    farmers.map { it.soulEggs.formatForDisplay() })
                append(farmer.soulEggs.formatForDisplay())
                if (index < farmers.size - 1) appendln()
            }
        }.toString().splitMessage(prefix = "Leader board continued…\n```", postfix = "```")

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