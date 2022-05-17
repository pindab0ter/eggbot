package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.associateAsync
import nl.pindab0ter.eggbot.helpers.compact
import nl.pindab0ter.eggbot.helpers.contract
import nl.pindab0ter.eggbot.helpers.multipartRespond
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.view.coopsInfoResponse
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@KordPreview
class CoopsInfoCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    class CoopsInfoArguments : Arguments() {
        val contract: Contract by contract()
        val compact: Boolean by compact()
    }

    override suspend fun setup() = config.servers.forEach { server ->
        publicSlashCommand(::CoopsInfoArguments) {
            name = "coops"
            description = "See how all the coops are doing."
            guild(server.snowflake)

            action {
                val contract = arguments.contract
                val compact = arguments.compact

                val coopStatuses = newSuspendedTransaction(null, databases[server.name]) {
                    val coops = Coop.find { Coops.contractId eq contract.id }.orderBy(Coops.name to ASC)

                    coops.associateAsync { coop ->
                        coop.name to AuxBrain.getCoopStatus(contract.id, coop.name)
                    }
                }

                if (coopStatuses.isEmpty()) {
                    respond { content = "No coops found for contract ${contract.id}" }
                    return@action
                }

                val contractCoopStatuses = coopStatuses.map { (coopName, coopStatus) ->
                    CoopContractStatus(contract, coopStatus, coopName)
                }

                guild?.coopsInfoResponse(
                    contract = contract,
                    statuses = contractCoopStatuses,
                    compact = compact
                )?.let { multipartRespond(it) }
            }
        }
    }
}
