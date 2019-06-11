package nl.pindab0ter.eggbot.auxbrain

sealed class CoopContractSimulationResult {
    data class InProgress(val simulation: CoopContractSimulation): CoopContractSimulationResult()
    data class Finished(val contractId: String, val coopId: String): CoopContractSimulationResult()
    data class NotFound(val contractId: String, val coopId: String): CoopContractSimulationResult()
    data class Empty(val contractId: String, val coopId: String): CoopContractSimulationResult()
}
