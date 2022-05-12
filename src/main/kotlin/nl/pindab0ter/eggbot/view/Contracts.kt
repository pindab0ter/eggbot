package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.configuredGuild
import nl.pindab0ter.eggbot.helpers.displayName
import nl.pindab0ter.eggbot.helpers.formatDayHourAndMinutes
import nl.pindab0ter.eggbot.helpers.toDateTime
import nl.pindab0ter.eggbot.model.Config


fun contractsResponse(soloContracts: List<Contract>, coopContracts: List<Contract>): String = buildString {
    suspend fun List<Contract>.printContracts(): String = buildString {
        this@printContracts.forEach { contract ->

            Config.eggsToEmotes[contract.egg]?.let { emojiSnowflake ->
                configuredGuild?.getEmojiOrNull(emojiSnowflake)
            }?.let { eggEmoji ->
                append(eggEmoji.mention)
            }
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