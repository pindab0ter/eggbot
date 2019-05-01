package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.database.Contract
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
        }.toString().splitMessage(prefix = "```", postfix = "```")

    fun earningsBonusLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Earnings Bonus",
        farmers.map { NameToValue(it.inGameName, it.earningsBonus.formatDecimal() + "\u00A0%") }
    )

    fun earningsBonusLeaderBoardCompact(farmers: List<Farmer>): List<String> = leaderBoard(
        "Earnings Bonus",
        farmers.map { NameToValue(it.inGameName, it.earningsBonus.formatIllions() + "\u00A0%") }
    )

    fun soulEggsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Soul Eggs",
        farmers.map { NameToValue(it.inGameName, it.soulEggs.formatDecimal()) }
    )

    fun prestigesLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.prestiges.formatDecimal()) }
    )

    fun droneTakedownsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.droneTakedowns.formatDecimal()) }
    )

    fun eliteDroneTakedownsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Elite Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.eliteDroneTakedowns.formatDecimal()) }
    )


    fun earningsBonus(farmer: Farmer): String = StringBuilder().apply {
        val eb = farmer.earningsBonus.formatInteger() + " %"
        val se = BigDecimal(farmer.soulEggs).formatInteger()
        val seToNext =
            farmer.nextRole
                ?.lowerBound
                ?.minus(farmer.earningsBonus)
                ?.divide(farmer.bonusPerSoulEgg, HALF_UP)
                ?.formatInteger() ?: "Unknown"
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

    fun contractStatus(
        localContract: EggInc.LocalContract,
        farm: EggInc.Simulation
    ): String = StringBuilder("**${localContract.contract.name}**:\n").apply {
        val eggs = localContract.myEggs
        val rate = 0.0
        val hourlyRate = rate.times(60)

        val elapsedTime = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())
        val timeRemaining = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)
        val requiredEggs = localContract.contract
            .goalsList[localContract.contract.goalsList.size - 1]
            .targetAmount
        val projectedEggs = rate.times(localContract.coopGracePeriodEndTime / 60)

        appendln("Eggs: ${eggs.formatIllions()}")
        appendln("Rate: ${rate.formatIllions(true)} (${hourlyRate.formatIllions(true)}/hr)")
        appendln("Time remaining: ${timeRemaining.asDayHoursAndMinutes()}")
        append("Projected eggs: ${projectedEggs.formatIllions(true)}")
        append("/")
        append("${requiredEggs.formatIllions(true)}\n")
    }.toString()

    fun coopStatus(
        contract: Contract,
        coopStatus: EggInc.CoopStatusResponse
    ): String = StringBuilder("`${contract.identifier}` (${contract.name}):\n").apply {
        val eggs = coopStatus.contributorsList.sumByDouble { it.contributionAmount }
        val rate = coopStatus.contributorsList.sumByDouble { it.contributionRate }
        val hourlyRate = rate.times(3600)
        val timeRemaining = coopStatus.secondsRemaining.toPeriod()
        val requiredEggs = contract.finalAmount
        val projectedEggs = coopStatus.contributorsList
            .sumByDouble { it.contributionRate }
            .times(coopStatus.secondsRemaining)

        appendln("**Co-op**: `${coopStatus.coopIdentifier}`")
        append("**Eggs**: ${eggs.formatIllions(true)}")
        append("${EggBot.jdaClient.getEmoteById(Config.eggEmojiIds[contract.egg])?.asMention ?: ""}\n")
        appendln("**Rate**: ${rate.formatIllions()} (${hourlyRate.formatIllions()}/hr)")
        appendln("**Time remaining**: ${timeRemaining.asDayHoursAndMinutes()}")
        append("**Projected eggs**: ${projectedEggs.formatIllions()}")
        append("/")
        append("${requiredEggs.formatIllions(true)}\n")
        appendln()
        appendln("Members (${coopStatus.contributorsCount}/${contract.maxCoopSize}):")
        appendln("```")
        // TODO: Goal reached at…
        // TODO: Add sleeping status
        val coopInfo = coopStatus.contributorsList.map {
            Triple(
                it.userName,
                it.contributionAmount.formatIllions(true),
                it.contributionRate.times(3600).formatIllions() + "/hr"
            )
        }
        coopInfo.forEach { (userName, amount, rate) ->
            append(userName)
            appendPaddingSpaces(userName, coopInfo.map { it.first })
            append(" ")
            appendPaddingSpaces(amount, coopInfo.map { it.second })
            append(amount)
            append("|")
            append(rate)
            appendln()
        }
        appendln("```")
    }.toString()
}