package nl.pindab0ter.eggbot.model.simulation.new

import org.joda.time.Duration

data class Farmer(
    val name: String,
    val initialState: FarmState, // TODO: Remove?
    val finalState: FarmState,
    val timeSinceBackup: Duration,
)