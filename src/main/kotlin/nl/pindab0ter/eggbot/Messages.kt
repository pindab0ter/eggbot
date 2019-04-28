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

fun earningsBonus(farmer: Farmer): String = StringBuilder().apply {
    val eb = farmer.earningsBonus.formatForDisplay()
    val ebToNext =
        farmer.nextRole
            ?.lowerBound
            ?.minus(farmer.earningsBonus)
            ?.formatForDisplay() ?: "Unknown"
    val role = farmer.role?.name ?: "Unknown"

    append("Earnings bonus for **${farmer.inGameName}**:\n")
    append("`Role:       ")
    append(" ".repeat(listOf(eb, ebToNext).maxBy { it.length }?.length?.minus(role.length) ?: 0))
    append("${farmer.role?.name ?: "Unknown"}`\n")
    append("`EB:         ")
    appendPaddingSpaces(eb, listOf(eb, ebToNext))
    append("$eb`\n")
    append("`EB to next: ")
    appendPaddingSpaces(ebToNext, listOf(eb, ebToNext))
    append("$ebToNext`")
}.toString()