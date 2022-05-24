package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.extensions.RemoveCoopsCommand.CleanupStatus
import nl.pindab0ter.eggbot.extensions.RemoveCoopsCommand.CleanupStatus.Status.*

fun removeCoopsResponse(contract: Contract, statuses: Map<String, CleanupStatus>) = buildString {

    append("Cleared ")

    when {
        statuses.count() == 1 -> append("1 coop")
        else -> append("${statuses.count()} co-ops")
    }

    appendLine(" for _${contract.name}_")

    if (statuses.all { (_, status) -> status.role == NO_ACTION && status.channel == NO_ACTION })
        appendLine("- No roles or channels to delete")

    if (statuses.any { (_, status) -> status.has(DELETED) })
        appendLine("- Deleted ${statuses.count { (_, status) -> status.role == DELETED }} roles and ${statuses.count { (_, status) -> status.channel == DELETED }} channels")

    if (statuses.any { (_, status) -> status.has(NOT_FOUND) })
        appendLine("- Could not find ${statuses.count { (_, status) -> status.role == NOT_FOUND }} roles and ${statuses.count { (_, status) -> status.channel == NOT_FOUND }} channels")

    if (statuses.any { (_, status) -> status.has(FAILED) })
        appendLine("- Failed to delete ${statuses.count { (_, status) -> status.role == FAILED }} roles and ${statuses.count { (_, status) -> status.channel == FAILED }} channels")

    appendLine()

    statuses
        .filter { (_, status) -> status.has(NOT_FOUND) || status.has(FAILED) }
        .toSortedMap()
        .forEach { (coopName, status) ->
            append("For `$coopName`: ")

            when (status.role) {
                NOT_FOUND -> append("Could not find role. ")
                else -> append("Failed to delete role. ")
            }

            when (status.channel) {
                NOT_FOUND -> append("Could not find channel.")
                else -> append("Failed to delete channel.")
            }

            appendLine()
        }
}
