package nl.pindab0ter.eggbot.model.simulation

data class Farmer(
    val name: String,
    val initialState: FarmState, // TODO: Remove?
    val finalState: FarmState,
)