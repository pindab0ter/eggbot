package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.helpers.NumberFormatter
import nl.pindab0ter.eggbot.helpers.appendPaddingCharacters
import nl.pindab0ter.eggbot.helpers.asIllions
import nl.pindab0ter.eggbot.model.database.Coop

fun rollCallResponse(
    contract: Contract,
    coops: List<Coop>,
): List<String> = listOf(buildString {
    appendLine("Co-ops generated for `${contract.id}`:")
    appendLine()

    append("```")
    coops.forEach { coop ->
        append(coop.name)
        appendPaddingCharacters(coop.name, coops.map { it.name })
        append(" (")
        appendPaddingCharacters(coop.farmers.count(), coops.map { it.farmers.count() })
        append(coop.farmers.count())
        append("/${contract.maxCoopSize} members): ")
        appendPaddingCharacters(
            coop.activeEarningsBonus.asIllions(NumberFormatter.INTEGER),
            coops.map { it.activeEarningsBonus.asIllions(NumberFormatter.INTEGER) })
        append(coop.activeEarningsBonus.asIllions(NumberFormatter.INTEGER) + " %")
        appendLine()
    }
    append("```")
}).plus(coops.map { coop ->
    val role = coop.roleId?.let { EggBot.guild.getRoleById(it) }
    buildString {
        appendLine("__**Co-op ${role?.asMention ?: coop.name} (`${coop.name}`)**__")
        coop.farmers.forEach { farmer ->
            append(
                EggBot.guild.getMemberById(farmer.discordUser.discordId)?.asMention
                    ?: farmer.discordUser.discordName
            )
            append(" (`${farmer.inGameName}`)")
            if (farmer.isActive.not()) append(" _Inactive_")
            appendLine()
        }
    }
})
