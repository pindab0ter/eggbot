package nl.pindab0ter.eggbot.helpers.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Level
import com.auxbrain.ei.Artifact.Level.*
import com.auxbrain.ei.Artifact.Name.*
import com.auxbrain.ei.Artifact.Rarity
import com.auxbrain.ei.Artifact.Rarity.*
import org.apache.logging.log4j.kotlin.Logging
import java.math.BigDecimal

object Artifacts : Logging {
    fun multiplierFor(
        artifact: Artifact,
        filter: List<Artifact.Name>,
    ): BigDecimal = artifactMultipliers.filter { (artifact, _) ->
        artifact.name in filter
    }[artifact.identifier] ?: BigDecimal.ONE.also {
        if (artifact.name in filter) logger.warn("Unknown artifact: ${artifact.rarity.name} ${artifact.level.name} ${artifact.name.name}")
    }

    val coopArtifacts: List<Artifact.Name> = EggLayingRate.coopEggLayingRateArtifacts

    private data class Identifier(
        val name: Artifact.Name,
        val level: Level,
        val rarity: Rarity,
    )

    private val Artifact.identifier
        get() = Identifier(name, level, rarity)

    // @formatter:off
    private val artifactMultipliers = hashMapOf(
        Identifier(GUSSET,            INFERIOR, COMMON)    to BigDecimal("1.05"),
        Identifier(GUSSET,            LESSER,   COMMON)    to BigDecimal("1.13"), // Game states 10%
        Identifier(GUSSET,            LESSER,   EPIC)      to BigDecimal("1.12"),
        Identifier(GUSSET,            NORMAL,   COMMON)    to BigDecimal("1.15"), // Game states 14%
        Identifier(GUSSET,            NORMAL,   RARE)      to BigDecimal("1.15"),
        Identifier(GUSSET,            GREATER,  LEGENDARY) to BigDecimal("1.30"),
        Identifier(QUANTUM_METRONOME, INFERIOR, COMMON)    to BigDecimal("1.05"),
        Identifier(QUANTUM_METRONOME, LESSER,   COMMON)    to BigDecimal("1.10"),
        Identifier(QUANTUM_METRONOME, LESSER,   RARE)      to BigDecimal("1.12"),
        Identifier(QUANTUM_METRONOME, NORMAL,   COMMON)    to BigDecimal("1.14"),
        Identifier(TACHYON_DEFLECTOR, INFERIOR, COMMON)    to BigDecimal("1.05"),
        Identifier(TACHYON_DEFLECTOR, LESSER,   COMMON)    to BigDecimal("1.10"),
        Identifier(TACHYON_STONE,     INFERIOR, COMMON)    to BigDecimal("1.01"),
        Identifier(TACHYON_STONE,     LESSER,   COMMON)    to BigDecimal("1.02"),
    )
    // @formatter:on
}