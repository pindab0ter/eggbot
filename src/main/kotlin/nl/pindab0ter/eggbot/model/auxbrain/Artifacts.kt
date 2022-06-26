package nl.pindab0ter.eggbot.model.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Level
import com.auxbrain.ei.Artifact.Level.*
import com.auxbrain.ei.Artifact.Name.*
import com.auxbrain.ei.Artifact.Rarity
import com.auxbrain.ei.Artifact.Rarity.*
import com.auxbrain.ei.Backup
import com.auxbrain.ei.Backup.Farm
import mu.KotlinLogging.logger
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.util.*

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
    get() = artifactMultipliers[identifier] ?: ONE.also {
        logger(Artifact::class.qualifiedName!!).warn {
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

    // Chalices

    Identifier(CHALICE,    INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(CHALICE,    LESSER,   COMMON)    to BigDecimal("1.10"),
    Identifier(CHALICE,    LESSER,   EPIC)      to BigDecimal("1.15"),
    Identifier(CHALICE,    NORMAL,   COMMON)    to BigDecimal("1.20"),
    Identifier(CHALICE,    NORMAL,   RARE)      to BigDecimal("1.23"),
    Identifier(CHALICE,    NORMAL,   EPIC)      to BigDecimal("1.25"),
    Identifier(CHALICE,    GREATER,  COMMON)    to BigDecimal("1.30"),
    Identifier(CHALICE,    GREATER,  EPIC)      to BigDecimal("1.35"),
    Identifier(CHALICE,    GREATER,  LEGENDARY) to BigDecimal("1.40"),

    // Gussets

    Identifier(GUSSET,               INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(GUSSET,               LESSER,   COMMON)    to BigDecimal("1.10"),
    Identifier(GUSSET,               LESSER,   EPIC)      to BigDecimal("1.12"),
    Identifier(GUSSET,               NORMAL,   COMMON)    to BigDecimal("1.15"),
    Identifier(GUSSET,               NORMAL,   RARE)      to BigDecimal("1.16"),
    Identifier(GUSSET,               GREATER,  COMMON)    to BigDecimal("1.20"),
    Identifier(GUSSET,               GREATER,  EPIC)      to BigDecimal("1.22"),
    Identifier(GUSSET,               GREATER,  LEGENDARY) to BigDecimal("1.25"),

    // Interstellar Compass

    Identifier(INTERSTELLAR_COMPASS, INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(INTERSTELLAR_COMPASS, LESSER,  COMMON)     to BigDecimal("1.10"),
    Identifier(INTERSTELLAR_COMPASS, NORMAL,  COMMON)     to BigDecimal("1.20"),
    Identifier(INTERSTELLAR_COMPASS, NORMAL,  RARE)       to BigDecimal("1.22"),
    Identifier(INTERSTELLAR_COMPASS, GREATER, COMMON)     to BigDecimal("1.30"),
    Identifier(INTERSTELLAR_COMPASS, GREATER, RARE)       to BigDecimal("1.35"),
    Identifier(INTERSTELLAR_COMPASS, GREATER, EPIC)       to BigDecimal("1.40"),
    Identifier(INTERSTELLAR_COMPASS, GREATER, LEGENDARY)  to BigDecimal("1.50"),

    // Life Stones

    Identifier(LIFE_STONE,           INFERIOR, COMMON)    to BigDecimal("1.02"),
    Identifier(LIFE_STONE,           LESSER,   COMMON)    to BigDecimal("1.03"),
    Identifier(LIFE_STONE,           NORMAL,   COMMON)    to BigDecimal("1.04"),

    // Quantum Metronomes

    Identifier(QUANTUM_METRONOME,    INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(QUANTUM_METRONOME,    LESSER,   COMMON)    to BigDecimal("1.10"),
    Identifier(QUANTUM_METRONOME,    LESSER,   RARE)      to BigDecimal("1.12"),
    Identifier(QUANTUM_METRONOME,    NORMAL,   COMMON)    to BigDecimal("1.15"),
    Identifier(QUANTUM_METRONOME,    NORMAL,   RARE)      to BigDecimal("1.17"),
    Identifier(QUANTUM_METRONOME,    NORMAL,   EPIC)      to BigDecimal("1.20"),
    Identifier(QUANTUM_METRONOME,    GREATER,  COMMON)    to BigDecimal("1.25"),
    Identifier(QUANTUM_METRONOME,    GREATER,  RARE)      to BigDecimal("1.27"),
    Identifier(QUANTUM_METRONOME,    GREATER,  EPIC)      to BigDecimal("1.30"),
    Identifier(QUANTUM_METRONOME,    GREATER,  LEGENDARY) to BigDecimal("1.35"),

    // Quantum Stones

    Identifier(QUANTUM_STONE,        INFERIOR, COMMON)    to BigDecimal("1.02"),
    Identifier(QUANTUM_STONE,        LESSER,   COMMON)    to BigDecimal("1.04"),
    Identifier(QUANTUM_STONE,        NORMAL,   COMMON)    to BigDecimal("1.05"),

    // Tachyon Deflectors

    Identifier(TACHYON_DEFLECTOR,    INFERIOR, COMMON)    to BigDecimal("1.05"),
    Identifier(TACHYON_DEFLECTOR,    LESSER,   COMMON)    to BigDecimal("1.08"),
    Identifier(TACHYON_DEFLECTOR,    NORMAL,   COMMON)    to BigDecimal("1.12"),
    Identifier(TACHYON_DEFLECTOR,    NORMAL,   RARE)      to BigDecimal("1.12"),
    Identifier(TACHYON_DEFLECTOR,    GREATER,  COMMON)    to BigDecimal("1.15"),
    Identifier(TACHYON_DEFLECTOR,    GREATER,  RARE)      to BigDecimal("1.17"),
    Identifier(TACHYON_DEFLECTOR,    GREATER,  EPIC)      to BigDecimal("1.19"),
    Identifier(TACHYON_DEFLECTOR,    GREATER,  LEGENDARY) to BigDecimal("1.20"),

    // Tachyon Stones

    Identifier(TACHYON_STONE,        INFERIOR, COMMON)    to BigDecimal("1.02"),
    Identifier(TACHYON_STONE,        LESSER,   COMMON)    to BigDecimal("1.04"),
    Identifier(TACHYON_STONE,        NORMAL,   COMMON)    to BigDecimal("1.05"),
)
// @formatter:on

fun Artifact.formatName() = name.name.lowercase(Locale.getDefault())
    .split("_")
    .joinToString(" ") { string -> string.replaceFirstChar { char -> char.titlecase() } }
