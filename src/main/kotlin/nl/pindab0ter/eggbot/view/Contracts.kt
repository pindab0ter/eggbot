package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import dev.kord.core.behavior.GuildBehavior
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.displayName
import nl.pindab0ter.eggbot.helpers.emoteMention
import nl.pindab0ter.eggbot.helpers.formatDayHourAndMinutes
import nl.pindab0ter.eggbot.helpers.toDateTime


fun GuildBehavior.contractsResponse(
    soloContracts: List<Contract>,
    coopContracts: List<Contract>,
): String = buildString {
    fun List<Contract>.printContracts(): String = buildString {
        this@printContracts.forEach { contract ->

            emoteMention(contract.egg)?.let { eggEmoji -> append(eggEmoji) }
            append("_${contract.name}_")
            append(" (")
            append(contract.egg.displayName)
            if (contract.coopAllowed) append(", max ${contract.maxCoopSize} farmers")
            append("). Valid until ")
            append(contract.expirationTime.toDateTime().formatDayHourAndMinutes())
            appendLine()
        }
    }

    runBlocking {
        if (soloContracts.isNotEmpty()) {
            append("\n__Solo contracts__:\n")
            append(soloContracts.printContracts())
        }
        if (coopContracts.isNotEmpty()) {
            append("\n__Co-op contracts__:\n")
            append(coopContracts.printContracts())
        }
    }
}