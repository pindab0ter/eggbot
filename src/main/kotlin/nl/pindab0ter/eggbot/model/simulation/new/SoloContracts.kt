package nl.pindab0ter.eggbot.model.simulation.new

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Contract
import com.auxbrain.ei.LocalContract
import nl.pindab0ter.eggbot.helpers.timeRemaining


fun simulateSoloContract(
    backup: Backup,
    contractId: String,
    catchUp: Boolean = true,
): SoloContractState {
    val localContract: LocalContract? = backup.contracts?.contracts?.find { contract ->
        contract.contract?.id == contractId
    }
    val contract: Contract? = localContract?.contract
    val farmer = Farmer(backup, contractId, catchUp)

    // TODO: Are these errors caught and handled?
    requireNotNull(localContract) { "Local contract information not found" }
    requireNotNull(contract) { "Contract information not found" }
    requireNotNull(farmer) { "Farm not found" }

    val contractState = SoloContractState(
        contractId = contract.id,
        contractName = contract.name,
        egg = contract.egg,
        goals = Goal.fromContract(contract, farmer.initialState.eggsLaid),
        timeRemaining = localContract.timeRemaining,
        farmer = farmer
    )

    return simulate(contractState)
}

