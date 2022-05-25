package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.entity.Role
import nl.pindab0ter.eggbot.NO_ALIAS
import nl.pindab0ter.eggbot.helpers.NumberFormatter.INTEGER
import nl.pindab0ter.eggbot.helpers.appendPaddingCharacters
import nl.pindab0ter.eggbot.helpers.formatIllions
import nl.pindab0ter.eggbot.helpers.mentionUser
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Farmer

suspend fun GuildBehavior.rollCallResponse(
    contract: Contract,
    coops: List<Coop>,
): List<String> {
    val header = listOf(buildString {
        appendLine("Co-ops generated for __${contract.name}__:")

        append("```")
        coops.forEach { coop ->
            append(coop.name)
            appendPaddingCharacters(coop.name, coops.map { it.name })
            append(" (")
            appendPaddingCharacters(coop.farmers.count(), coops.map { it.farmers.count() })
            append(coop.farmers.count())
            append("/${contract.maxCoopSize} members): ")
            appendPaddingCharacters(
                coop.activeEarningsBonus.formatIllions(INTEGER),
                coops.map { it.activeEarningsBonus.formatIllions(INTEGER) })
            append(coop.activeEarningsBonus.formatIllions(INTEGER) + " %")
            appendLine()
        }
        appendLine("```")
    })

    val coopContent = coops.map { coop ->
        val role = coop.roleId?.let { getRoleOrNull(it) }
        buildString {
            // Header
            appendLine("**__Co-op ${role?.mention ?: coop.name} (`${coop.name}`)__**")

            // Body
            coop.farmers
                .sortedBy { farmer -> (farmer.inGameName ?: "") }
                .forEach { farmer ->
                    if (farmer.isActive.not()) append("_")
                    append(mentionUser(farmer.discordUser.snowflake))
                    append(" (`${farmer.inGameName ?: NO_ALIAS}`)")
                    if (farmer.isActive.not()) append(" (Inactive)_")
                    appendLine()
                }
            appendLine()
        }
    }

    return header + coopContent
}

suspend fun GuildBehavior.coopChannelMessage(
    coop: Coop,
    role: Role?,
): String = buildString {
    // Header
    appendLine("**__Co-op ${role?.mention ?: coop.name} (`${coop.name}`)__**")

    // Body
    coop.farmers
        .sortedBy(Farmer::earningsBonus)
        .forEach { farmer ->
            append(asGuildOrNull()?.mentionUser(farmer.discordUser.snowflake))
            append(" (`${farmer.inGameName ?: NO_ALIAS}`, ")
            append("${farmer.earningsBonus.formatIllions(INTEGER)} %)")
            appendLine()
        }
    appendLine()
}
