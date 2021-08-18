package nl.pindab0ter.eggbot.kord.commands

import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.coopIdChoice
import nl.pindab0ter.eggbot.helpers.discard
import nl.pindab0ter.eggbot.helpers.publicMultipartFollowUp
import nl.pindab0ter.eggbot.kord.converters.contractChoice
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InActive.*
import nl.pindab0ter.eggbot.model.simulation.Farmer
import nl.pindab0ter.eggbot.view.coopFinishedIfBankedResponse
import nl.pindab0ter.eggbot.view.coopFinishedResponse
import nl.pindab0ter.eggbot.view.coopInfoResponse

class CoopArguments : Arguments() {
    val contract: Contract by contractChoice()
    val coopId: String by coopIdChoice()
    val compact: Boolean by defaultingBoolean(
        displayName = "compact",
        description = "Better fit output for mobile devices",
        defaultValue = false,
    )
}

@KordPreview
val coopCommand: suspend SlashCommand<out CoopArguments>.() -> Unit = {
    name = "coop"
    description = "Shows info on a specific co-op, displaying the current status and player contribution."
    autoAck = PUBLIC

    action {
        val contract = arguments.contract
        val coopStatus = AuxBrain.getCoopStatus(arguments.contract.id, arguments.coopId)
            ?: return@action publicFollowUp {
                content = "No co-op found for contract __${contract.name}__ with ID `${arguments.coopId}`"
            }.discard()
        val compact = arguments.compact

        when (val status = CoopContractStatus(contract, coopStatus, arguments.coopId)) {
            is Abandoned -> publicFollowUp {
                content = """
                        `${status.coopStatus.coopId}` vs. __${contract.name}__:
                            
                        This co-op has no members.""".trimIndent()
            }

            is Failed -> publicFollowUp {
                content = """
                        `${status.coopStatus.coopId}` vs. __${contract.name}__:
                            
                        This co-op has not reached their final goal.""".trimIndent()
            }

            is Finished -> publicMultipartFollowUp(coopFinishedResponse(coopStatus, contract, compact))

            is InProgress -> {
                val sortedState = status.state.copy(
                    farmers = status.state.farmers.sortedByDescending(Farmer::currentEggsLaid)
                )

                publicMultipartFollowUp(when (status) {
                    is InProgress.FinishedIfBanked -> coopFinishedIfBankedResponse(sortedState, compact)
                    else -> coopInfoResponse(sortedState, compact)
                })
            }
        }
    }
}
