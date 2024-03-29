package nl.pindab0ter.eggbot.model.auxbrain

import com.auxbrain.ei.Backup
import com.auxbrain.ei.CoopStatus
import org.joda.time.Duration

object Silos {
    fun maxAwayTimeFor(backup: Backup, farm: Backup.Farm): Duration = Duration.standardHours(1L)
        .plus(backup.extraAwayTimePerSilo)
        .multipliedBy(farm.silosOwned.toLong())

    private val Backup.extraAwayTimePerSilo: Duration
        get() = Duration.ZERO.withDurationAdded(
            Duration.standardMinutes(6L),
            game.epicResearch[EpicResearch.SILO_CAPACITY.ordinal].level
        )
}

val CoopStatus.FarmInfo.maxAwayTime: Duration
    get() = Duration.standardHours(1L)
        .plus(extraAwayTimePerSilo)
        .multipliedBy(silosOwned.toLong())

private val CoopStatus.FarmInfo.extraAwayTimePerSilo: Duration
    get() = Duration.ZERO.withDurationAdded(
        Duration.standardMinutes(6L),
        epicResearch[EpicResearch.SILO_CAPACITY.ordinal].level
    )
