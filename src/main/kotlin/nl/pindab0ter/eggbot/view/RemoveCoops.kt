package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.extensions.RemoveCoopsCommand

fun removeCoopsResponse(contract: Contract, statuses: List<Pair<String, Set<RemoveCoopsCommand.DeletionStatus>>>) = buildString {
    val successfullyDeletedChannels = statuses.count { (_, statuses: Set<RemoveCoopsCommand.DeletionStatus>) ->
        statuses.any { deletionStatus: RemoveCoopsCommand.DeletionStatus ->
            deletionStatus.type == RemoveCoopsCommand.DeletionStatus.Type.CHANNEL && deletionStatus.deleted
        }
    }
    val successfullyDeletedRoles = statuses.count { (_, statuses: Set<RemoveCoopsCommand.DeletionStatus>) ->
        statuses.any { deletionStatus: RemoveCoopsCommand.DeletionStatus ->
            deletionStatus.type == RemoveCoopsCommand.DeletionStatus.Type.ROLE && deletionStatus.deleted
        }
    }

    appendLine("Cleared the roll-call for __${contract.name}__:")
    appendLine("Successfully deleted $successfullyDeletedChannels channels and $successfullyDeletedRoles roles.")

    statuses
        .map { (coopName, statuses) ->
            coopName to statuses
                .filterNot(RemoveCoopsCommand.DeletionStatus::deleted)
                .map { deletionStatus -> deletionStatus.type }
                .sorted()
        }
        .filter { (_, statuses) -> statuses.isNotEmpty() }
        .sortedWith(compareBy { it.first })
        .let { failedToDelete ->
            if (failedToDelete.isNotEmpty()) appendLine("Failed to delete:")
            failedToDelete.forEach { (coopName, types) ->
                append("For `$coopName`: ")
                when (types.size) {
                    1 -> append(types.first().name.lowercase())
                    else -> types.joinToString(" and ") { type -> type.name.lowercase() }
                }
            }
        }
}
