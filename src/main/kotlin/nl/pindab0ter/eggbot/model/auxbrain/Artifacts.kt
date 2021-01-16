package nl.pindab0ter.eggbot.model.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Level
import com.auxbrain.ei.Artifact.Level.*
import com.auxbrain.ei.Artifact.Name.*
import com.auxbrain.ei.Artifact.Rarity
import com.auxbrain.ei.Artifact.Rarity.*
import com.auxbrain.ei.Backup
import com.auxbrain.ei.Backup.Farm
import org.apache.logging.log4j.kotlin.logger
import java.math.BigDecimal

fun Backup.artifactsFor(farm: Farm?): List<Artifact> = artifactsDatabase?.activeArtifactSets
    ?.getOrNull(farms.indexOf(farm))?.slots?.flatMap { activeArtifactSlot ->
        artifactsDatabase.inventoryItems.find { artifactInventoryItem ->
            artifactInventoryItem.itemId == activeArtifactSlot.itemId
        }?.artifact?.let { artifact ->
            artifact.stones.plus(artifact.artifactBase).filterNotNull()
        }.orEmpty()
    }.orEmpty()

fun Backup.coopArtifactsFor(farm: Farm?): List<Artifact> = artifactsFor(farm).filter { artifact ->
    artifact.name in coopArtifacts
}

val Artifact.multiplier: BigDecimal
    get() = artifactMultipliers[identifier] ?: BigDecimal.ONE.also {
        logger(Artifact::class.qualifiedName!!).warn { // TODO: Verify this works
            "Unknown artifact: ${rarity.name} ${level.name} ${name.name}"
        }
    }

private val coopArtifacts: List<Artifact.Name> = listOf(TACHYON_DEFLECTOR)

private data class Identifier(
    val name: Artifact.Name,
    val level: Level,
    val rarity: Rarity,
)

private val Artifact.identifier
    get() = Identifier(name, level, rarity)

// @formatter:off
private val artifactMultipliers = hashMapOf(
    Identifier(GUSSET,               INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(GUSSET,               LESSER,   COMMON)    to BigDecimal("1.10"),
    // Identifier(GUSSET,               LESSER,   RARE)      to BigDecimal("?"),
    Identifier(GUSSET,               LESSER,   EPIC)      to BigDecimal("1.12"),
    Identifier(GUSSET,               NORMAL,   COMMON)    to BigDecimal("1.14"),
    Identifier(GUSSET,               NORMAL,   RARE)      to BigDecimal("1.15"),
    // Identifier(GUSSET,               NORMAL,   EPIC)      to BigDecimal("?"),
    Identifier(GUSSET,               GREATER,  COMMON)    to BigDecimal("1.20"),
    Identifier(GUSSET,               GREATER,  LEGENDARY) to BigDecimal("1.30"),
    Identifier(QUANTUM_METRONOME,    INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(QUANTUM_METRONOME,    LESSER,   COMMON)    to BigDecimal("1.10"),
    Identifier(QUANTUM_METRONOME,    LESSER,   RARE)      to BigDecimal("1.12"),
    Identifier(QUANTUM_METRONOME,    NORMAL,   COMMON)    to BigDecimal("1.14"),
    Identifier(QUANTUM_METRONOME,    NORMAL,   RARE)      to BigDecimal("1.17"),
    Identifier(TACHYON_DEFLECTOR,    INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(TACHYON_DEFLECTOR,    LESSER,   COMMON)    to BigDecimal("1.10"),
    // Identifier(TACHYON_DEFLECTOR,    LESSER,   RARE)      to BigDecimal("?"),
    // Identifier(TACHYON_DEFLECTOR,    LESSER,   EPIC)      to BigDecimal("?"),
    Identifier(TACHYON_STONE,        INFERIOR, COMMON)    to BigDecimal("1.02"),
    Identifier(TACHYON_STONE,        LESSER,   COMMON)    to BigDecimal("1.04"),
    Identifier(TACHYON_STONE,        NORMAL,   COMMON)    to BigDecimal("1.05"),
    Identifier(INTERSTELLAR_COMPASS, INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(INTERSTELLAR_COMPASS, LESSER, COMMON)      to BigDecimal("1.10"),
    // Identifier(INTERSTELLAR_COMPASS, LESSER, RARE)        to BigDecimal("?"),
    // Identifier(INTERSTELLAR_COMPASS, LESSER, EPIC)        to BigDecimal("?"),
    Identifier(INTERSTELLAR_COMPASS, NORMAL, COMMON)      to BigDecimal("1.20"),
    Identifier(INTERSTELLAR_COMPASS, NORMAL, RARE)        to BigDecimal("1.22"),
    // Identifier(INTERSTELLAR_COMPASS, NORMAL, EPIC)        to BigDecimal("?"),
    Identifier(INTERSTELLAR_COMPASS, GREATER, COMMON)     to BigDecimal("1.30"),
    // Identifier(INTERSTELLAR_COMPASS, GREATER, RARE)       to BigDecimal("?"),
    // Identifier(INTERSTELLAR_COMPASS, GREATER, EPIC)       to BigDecimal("?"),
    // Identifier(INTERSTELLAR_COMPASS, GREATER, LEGENDARY)  to BigDecimal("?"),
)
// @formatter:on