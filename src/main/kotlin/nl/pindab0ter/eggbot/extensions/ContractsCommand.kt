package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.view.contractsResponse

@KordPreview
class ContractsCommand : Extension() {
    override val name: String = javaClass.simpleName

    override suspend fun setup() = config.servers.forEach { server ->
        publicSlashCommand {
            name = "contracts"
            description = "List all currently available contracts."
            guild(server.snowflake)

            action {
                val contracts = AuxBrain.getContracts()
                val soloContracts = contracts.filter { contract -> !contract.coopAllowed }
                val coopContracts = contracts.filter { contract -> contract.coopAllowed }

                respond { content = guild?.contractsResponse(soloContracts, coopContracts) }
            }
        }
    }
}
