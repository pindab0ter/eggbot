package nl.pindab0ter.eggbot.helpers

import com.auxbrain.ei.*
import nl.pindab0ter.eggbot.helpers.auxbrain.Artifacts
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal

fun Backup.farmFor(contractId: String): Backup.Farm? = farms.firstOrNull { farm ->
    farm.contractId == contractId
}

private fun Backup.activeArtifactsFor(
    farm: Backup.Farm?,
) = artifactsDatabase?.activeArtifactSets?.get(farms.indexOf(farm))?.slots?.flatMap { activeArtifactSlot ->
    artifactsDatabase.inventoryItems.find { artifactInventoryItem ->
        artifactInventoryItem.itemId == activeArtifactSlot.itemId
    }?.artifact?.let { artifact ->
        artifact.stones.plus(artifact.artifactBase).filterNotNull()
    }.orEmpty()
}

fun Backup.activeSoloArtifactsFor(farm: Backup.Farm?): List<Artifact> =
    activeArtifactsFor(farm)?.filter { artifact ->
        Artifacts.coopArtifacts.contains(artifact.name).not()
    } ?: emptyList()

fun Backup.activeCoopArtifactsFor(farm: Backup.Farm?): List<Artifact> =
    activeArtifactsFor(farm)?.filter { artifact ->
        Artifacts.coopArtifacts.contains(artifact.name)
    } ?: emptyList()

val Backup.Farm.timeSinceLastStep: Duration
    get() = Duration(lastStepTime.toDateTime(), DateTime.now())
val Backup.timeSinceBackup: Duration
    get() = Duration(approxTime.toDateTime(), DateTime.now())
val Backup.Game.soulEggResearchLevel: Int
    get() = epicResearch.find { it.id == "soul_eggs" }!!.level
val Backup.Game.prophecyEggResearchLevel: Int
    get() = epicResearch.find { it.id == "prophecy_bonus" }!!.level
val Contract.finalGoal: BigDecimal
    get() = BigDecimal(goals.maxByOrNull { it.targetAmount }!!.targetAmount)
val LocalContract.finished: Boolean
    get() = lastAmountWhenRewardGiven.toBigDecimal() >= contract?.finalGoal
val LocalContract.timeRemaining: Duration
    get() = contract!!.lengthSeconds.toDuration().minus(Duration(timeAccepted.toDateTime(), DateTime.now()))
val CoopStatus.eggsLaid: BigDecimal
    get() = BigDecimal(totalAmount)
val CoopStatus.timeRemaining: Duration
    get() = secondsRemaining.toDuration()
