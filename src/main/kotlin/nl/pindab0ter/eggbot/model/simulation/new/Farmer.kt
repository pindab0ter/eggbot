package nl.pindab0ter.eggbot.model.simulation.new

data class Farmer(
    val name: String,
    val initialState: FarmState, // TODO: Remove?
    val finalState: FarmState,
)