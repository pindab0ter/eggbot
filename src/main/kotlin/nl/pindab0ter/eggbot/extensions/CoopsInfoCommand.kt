package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.compact
import nl.pindab0ter.eggbot.helpers.contract
import nl.pindab0ter.eggbot.helpers.guilds
import nl.pindab0ter.eggbot.helpers.multipartRespond
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.view.coopsInfoResponse
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class CoopsInfoCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() {
        class CoopsInfoArguments : Arguments() {
            val contract: Contract by contract()
            val compact: Boolean by compact()
        }

        for (guild in guilds) publicSlashCommand(::CoopsInfoArguments) {
            name = "coops"
            description = "See how all the coops are doing."
            guild(guild.id)

            action {
                val contract = arguments.contract
                val compact = arguments.compact

                val coops = transaction {
                    Coop.find { Coops.contractId eq contract.id }.orderBy(Coops.name to ASC)
                }

                val coopStatuses = coops.associate { coop ->
                    coop.name to AuxBrain.getCoopStatus(contract.id, coop.name)
                }

                if (coopStatuses.isEmpty()) {
                    respond { content = "No coops found for contract ${contract.id}" }
                    return@action
                }

                val contractCoopStatuses = coopStatuses.map { (coopName, coopStatus) ->
                    CoopContractStatus(contract, coopStatus, coopName)
                }

                multipartRespond(
                    guild.coopsInfoResponse(
                        contract = contract,
                        statuses = contractCoopStatuses,
                        compact = compact
                    )
                )
            }
        }
    }
}
