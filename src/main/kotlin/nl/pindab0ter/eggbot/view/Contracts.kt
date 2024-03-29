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
            append("__${contract.name}__")
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
            append("\n**Solo contracts**:\n")
            append(soloContracts.printContracts())
        }
        if (coopContracts.isNotEmpty()) {
            append("\n**Co-op contracts**:\n")
            append(coopContracts.printContracts())
        }
    }
}