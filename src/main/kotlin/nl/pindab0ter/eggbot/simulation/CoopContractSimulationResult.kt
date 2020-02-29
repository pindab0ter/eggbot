package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc

sealed class CoopContractSimulationResult {
    data class NotFound(
        val contractId: String,
        val coopId: String
    ) : CoopContractSimulationResult()

    data class Abandoned(
        val coopStatus: EggInc.CoopStatusResponse,
        val contractName: String
    ) : CoopContractSimulationResult()

    data class Failed(
        val coopStatus: EggInc.CoopStatusResponse,
        val contractName: String
    ) : CoopContractSimulationResult()

    data class Finished(
        val coopStatus: EggInc.CoopStatusResponse,
        val contractName: String
    ) : CoopContractSimulationResult()

    data class InProgress(
        val simulation: CoopContractSimulation
    ) : CoopContractSimulationResult()
}
