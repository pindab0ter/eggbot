package nl.pindab0ter.eggbot.model.simulation.new

import org.joda.time.Duration

data class Farmer(
    val name: String,
    val initialState: FarmState,
    val finalState: FarmState,
    val constants: Constants,
    val timeSinceBackup: Duration,
)