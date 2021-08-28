package nl.pindab0ter.eggbot.commands

import com.auxbrain.ei.Contract
import com.auxbrain.ei.CoopStatus
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.*
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission.*
import dev.kord.core.behavior.createRole
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import kotlinx.coroutines.flow.firstOrNull
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config.emojiWarning
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Coops
import nl.pindab0ter.eggbot.model.database.Farmers
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InActive.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress
import nl.pindab0ter.eggbot.view.coopFinishedIfBankedResponse
import nl.pindab0ter.eggbot.view.coopFinishedResponse
import nl.pindab0ter.eggbot.view.coopInfoResponse
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import nl.pindab0ter.eggbot.model.database.Farmer.Companion as DatabaseFarmer
import nl.pindab0ter.eggbot.model.simulation.Farmer as SimulationFarmer

class CoopInfoArguments : Arguments() {
    val contract: Contract by contractChoice()
    val coopId: String by coopId()
    val compact: Boolean by compact()
}

@KordPreview
val coopInfoCommand: suspend SlashCommand<out CoopInfoArguments>.() -> Unit = {
    name = "info"
    description = "See the current status and player contribution of a specific co-op."
    autoAck = NONE

    action {
        val contract = arguments.contract
        val coopStatus = AuxBrain.getCoopStatus(arguments.contract.id, arguments.coopId)
            ?: return@action ack(true).also {
                ephemeralFollowUp {
                    content = "No co-op found for contract _${contract.name}_ with ID `${arguments.coopId}`"
                }
            }.discard()
        val compact = arguments.compact

        ack(false)
        when (val status = CoopContractStatus(contract, coopStatus, arguments.coopId)) {
            is Abandoned -> publicFollowUp {
                content = """
                        `${status.coopStatus.coopId}` vs. _${contract.name}_:
                            
                        This co-op has no members.""".trimIndent()
            }

            is Failed -> publicFollowUp {
                content = """
                        `${status.coopStatus.coopId}` vs. _${contract.name}_:
                            
                        This co-op has not reached their final goal.""".trimIndent()
            }

            is Finished -> publicMultipartFollowUp(coopFinishedResponse(coopStatus, contract, compact))

            is InProgress -> {
                val sortedState = status.state.copy(
                    farmers = status.state.farmers.sortedByDescending(SimulationFarmer::currentEggsLaid)
                )

                publicMultipartFollowUp(when (status) {
                    is InProgress.FinishedIfBanked -> coopFinishedIfBankedResponse(sortedState, compact)
                    else -> coopInfoResponse(sortedState, compact)
                })
            }
        }
    }
}
