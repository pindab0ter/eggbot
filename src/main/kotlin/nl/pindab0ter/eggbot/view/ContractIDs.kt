package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.helpers.formatDaysHoursAndMinutes
import nl.pindab0ter.eggbot.helpers.toDateTime
import org.joda.time.DateTime
import org.joda.time.Duration


fun contractIDsResponse(soloContracts: List<Contract>, coopContracts: List<Contract>): String = buildString {
    appendLine("IDs for currently active contracts:")

    fun List<Contract>.printContracts(): String = buildString {
        this@printContracts.forEach { contract ->
            append("**`${contract.id}`**: ")
            append("${contract.name} ")
            append("(")
            //TODO: append(EggBot.eggsToEmotes[contract.egg]?.asMention ?: contract.egg.name)
            append("(${contract.egg.name})")
            if (contract.maxCoopSize > 1) append(", max ${contract.maxCoopSize} farmers")
            else append(", solo")
            append(")")
            append(", _valid for ")
            append(
                Duration(DateTime.now(), contract.expirationTime.toDateTime()).formatDaysHoursAndMinutes()
            )
            appendLine("_")
        }
    }

    if (soloContracts.isNotEmpty()) {
        append("\n  __Solo contracts__:\n")
        append(soloContracts.printContracts())
    }
    if (coopContracts.isNotEmpty()) {
        append("\n  __Co-op contracts__:\n")
        append(coopContracts.printContracts())
    }
}