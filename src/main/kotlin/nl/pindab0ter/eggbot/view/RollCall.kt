package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import dev.kord.core.entity.Guild
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.NumberFormatter
import nl.pindab0ter.eggbot.helpers.appendPaddingCharacters
import nl.pindab0ter.eggbot.helpers.formatIllions
import nl.pindab0ter.eggbot.model.database.Coop
import org.jetbrains.exposed.sql.transactions.transaction

fun Guild.rollCallResponse(
    contract: Contract,
    coops: List<Coop>,
): List<String> = runBlocking {
    val header = transaction {
        listOf(buildString {
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
                    coop.activeEarningsBonus.formatIllions(NumberFormatter.INTEGER),
                    coops.map { it.activeEarningsBonus.formatIllions(NumberFormatter.INTEGER) })
                append(coop.activeEarningsBonus.formatIllions(NumberFormatter.INTEGER) + " %")
                appendLine()
            }
            appendLine("```")
        })
    }

    val coopContent = coops.map { coop ->
        val role = coop.roleSnowflake?.let { getRoleOrNull(it) }
        buildString {
            // Header
            appendLine("**__Co-op ${role?.mention ?: coop.name} (`${coop.name}`)__**")

            // Body
            coop.farmers
                .sortedBy { farmer -> farmer.inGameName }
                .forEach { farmer ->
                    if (farmer.isActive.not()) append("_")
                    append(farmer.discordUser.mention)
                    if (farmer.inGameName.isNotEmpty()) append(" (`${farmer.inGameName}`)")
                    if (farmer.isActive.not()) append(" (Inactive)_")
                    appendLine()
                }
            appendLine()
        }
    }

    return@runBlocking header + coopContent
}
