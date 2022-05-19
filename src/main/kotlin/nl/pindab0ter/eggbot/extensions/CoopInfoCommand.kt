package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission.SendMessages
import dev.kord.common.entity.Permission.ViewChannel
import dev.kord.common.entity.optional.firstOrNull
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.compact
import nl.pindab0ter.eggbot.helpers.contract
import nl.pindab0ter.eggbot.helpers.multipartRespond
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InActive.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.NotFound
import nl.pindab0ter.eggbot.model.simulation.Farmer
import nl.pindab0ter.eggbot.view.coopFinishedIfBankedResponse
import nl.pindab0ter.eggbot.view.coopFinishedResponse
import nl.pindab0ter.eggbot.view.coopInfoResponse
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Integer.min

@KordPreview
class CoopInfoCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() = config.servers.forEach { server ->
        class CoopInfoArguments : Arguments() {
            val contract: Contract by contract()
            val coopId: String by string {
                name = "coop"
                description = "The co-op ID. Can be found in #roll-call or in-game."

                autoComplete {
                    val contractInput = it.interaction.data.data.options.firstOrNull { optionData ->
                        optionData.name == "contract"
                    }?.value?.value?.value as String?

                    val coopInput = it.interaction.data.data.options.firstOrNull { optionData ->
                        optionData.name == "coop"
                    }?.value?.value?.value as String?

                    if (contractInput !== null) {
                        val coops = transaction(databases[server.name]) {
                            val contract = AuxBrain.getContracts().find { contract -> contract.name == contractInput }
                            if (contract != null) {
                                Coop
                                    .find { (Coops.contractId eq contract.id) }
                                    .orderBy(Coops.name to SortOrder.ASC)
                                    .filter { coop -> coopInput != null && coop.name.contains(coopInput, ignoreCase = true) }
                                    .run { subList(0, min(count(), 25)) } // Limit to 25 results
                                    .associate { coop -> Pair(coop.name, coop.name) }
                            } else {
                                emptyMap()
                            }
                        }
                        suggestStringMap(coops)
                    }
                }

                validate {
                    failIf(value.contains(" "), "Co-op ID cannot contain spaces.")
                }
            }
            val compact: Boolean by compact()
        }

        publicSlashCommand(::CoopInfoArguments) {
            name = "coop"
            description = "See the current status and player contribution of a specific co-op."
            guild(server.snowflake)
            requireBotPermissions(
                ViewChannel,
                SendMessages,
            )

            action {
                val contract = arguments.contract
                val coopStatus = AuxBrain.getCoopStatus(arguments.contract.id, arguments.coopId)
                val compact = arguments.compact

                if (coopStatus == null) {
                    respond {
                        content = "No co-op found for contract _${contract.name}_ with ID `${arguments.coopId}`"
                    }
                    return@action
                }

                when (val coopContractStatus = CoopContractStatus(contract, coopStatus, arguments.coopId, databases[server.name])) {
                    is NotFound -> respond {
                        content = "No co-op found for contract _${contract.name}_ with ID `${arguments.coopId}`"
                    }
                    is Abandoned -> respond {
                        content = """
                                `${coopContractStatus.coopStatus.coopId}` vs. _${contract.name}_:
                                    
                                This co-op has no members.""".trimIndent()
                    }

                    is Failed -> respond {
                        content = """
                                `${coopContractStatus.coopStatus.coopId}` vs. _${contract.name}_:
                                    
                                This co-op has not reached their final goal.""".trimIndent()
                    }

                    is Finished -> multipartRespond(coopFinishedResponse(coopStatus, contract, compact))

                    is InProgress -> {
                        val sortedState = coopContractStatus.state.copy(
                            farmers = coopContractStatus.state.farmers.sortedByDescending(Farmer::currentEggsLaid)
                        )

                        multipartRespond(
                            when (coopContractStatus) {
                                is InProgress.FinishedIfBanked -> coopFinishedIfBankedResponse(sortedState, compact)
                                else -> coopInfoResponse(sortedState, compact)
                            }
                        )
                    }
                }
            }
        }
    }
}
