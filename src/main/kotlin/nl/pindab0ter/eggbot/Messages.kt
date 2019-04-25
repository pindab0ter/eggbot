package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.database.Farmer

fun leaderBoard(farmers: List<Farmer>): List<String> = StringBuilder("Earnings Bonus leader board:\n").apply {
    append("```")
    farmers.forEachIndexed { index, farmer ->
        append("${index + 1}:")
        appendPaddingSpaces(index + 1, farmers.count())
        append(" ")
        append(farmer.inGameName)
        appendPaddingSpaces(farmer.inGameName, farmers.map { it.inGameName })
        append(" ")
        appendPaddingSpaces(
            farmer.earningsBonus.formatForDisplay(),
            farmers.map { it.earningsBonus.formatForDisplay() })
        append(farmer.earningsBonus.formatForDisplay())
        appendln()
    }
}.toString().splitMessage(prefix = "Leader board continued…\n```", postfix = "```")