package nl.pindab0ter.eggbot

import com.auxbrain.ei.Egg.*
import com.charleskorn.kaml.Yaml
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.Result.Failure
import com.github.kittinunf.result.Result.Success
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.File

val logger = KotlinLogging.logger {}

@Serializable
data class Config(
    val botToken: String,
    private val botOwnerId: String,
    val eggIncId: String,
    val deviceId: String,

    val sentryDsn: String?,

    val servers: List<Server>
) {
    val botOwner: Snowflake get() = Snowflake(botOwnerId)
}

@SerialName("server")
@Serializable
class Server(
    val name: String,
    private val id: String,
    val databaseJdbcUrlEnv: String,
    val role: Role,
    val channel: Channel,
    val emote: Emote,
) {
    val snowflake: Snowflake get() = Snowflake(this.id)

}

// @formatter:off
val Server.eggsToEmotes get() = mapOf (
    UNKNOWN_EGG    to emote.defaultEgg,
    EDIBLE         to emote.edibleEgg,
    SUPERFOOD      to emote.superfoodEgg,
    MEDICAL        to emote.medicalEgg,
    ROCKET_FUEL    to emote.rocketFuelEgg,
    SUPER_MATERIAL to emote.superMaterialEgg,
    FUSION         to emote.fusionEgg,
    QUANTUM        to emote.quantumEgg,
    IMMORTALITY    to emote.immortalityEgg,
    TACHYON        to emote.tachyonEgg,
    GRAVITON       to emote.gravitonEgg,
    DILITHIUM      to emote.dilithiumEgg,
    PRODIGY        to emote.prodigyEgg,
    TERRAFORM      to emote.terraformEgg,
    ANTIMATTER     to emote.antimatterEgg,
    DARK_MATTER    to emote.darkMatterEgg,
    AI             to emote.aiEgg,
    NEBULA         to emote.nebulaEgg,
    UNIVERSE       to emote.universeEgg,
    ENLIGHTENMENT  to emote.enlightenmentEgg,
    CHOCOLATE      to emote.chocolateEgg,
    EASTER         to emote.easterEgg,
    WATER_BALLOON  to emote.waterBalloonEgg,
    FIREWORK       to emote.fireworkEgg,
    PUMPKIN        to emote.pumpkinEgg
)
// @formatter:on

@SerialName("role")
@Serializable
class Role(
    private val adminId: String,
) {
    val admin get() = Snowflake(this.adminId)
}

@SerialName("channel")
@Serializable
class Channel(
    private val coopsGroupId: String,
    private val earningsBonusLeaderBoardId: String?,
    private val soulEggsLeaderBoardId: String?,
    private val prestigesLeaderBoardId: String?,
    private val droneTakedownsLeaderBoardId: String?,
    private val eliteDroneTakedownsLeaderBoardId: String?,
) {
    val coopsGroup get() = Snowflake(coopsGroupId)
    val earningsBonusLeaderBoard get() = earningsBonusLeaderBoardId?.let { Snowflake(it) }
    val soulEggsLeaderBoard get() = soulEggsLeaderBoardId?.let { Snowflake(it) }
    val prestigesLeaderBoard get() = prestigesLeaderBoardId?.let { Snowflake(it) }
    val droneTakedownsLeaderBoard get() = droneTakedownsLeaderBoardId?.let { Snowflake(it) }
    val eliteDroneTakedownsLeaderBoard get() = eliteDroneTakedownsLeaderBoardId?.let { Snowflake(it) }
}

@SerialName("emote")
@Serializable
data class Emote(
    private val defaultEggId: String?,
    private val edibleEggId: String?,
    private val superfoodEggId: String?,
    private val medicalEggId: String?,
    private val rocketFuelEggId: String?,
    private val superMaterialEggId: String?,
    private val fusionEggId: String?,
    private val quantumEggId: String?,
    private val immortalityEggId: String?,
    private val tachyonEggId: String?,
    private val gravitonEggId: String?,
    private val dilithiumEggId: String?,
    private val prodigyEggId: String?,
    private val terraformEggId: String?,
    private val antimatterEggId: String?,
    private val darkMatterEggId: String?,
    private val aiEggId: String?,
    private val nebulaEggId: String?,
    private val universeEggId: String?,
    private val enlightenmentEggId: String?,
    private val chocolateEggId: String?,
    private val easterEggId: String?,
    private val waterBalloonEggId: String?,
    private val fireworkEggId: String?,
    private val pumpkinEggId: String?,
    private val goldEggId: String?,
    private val soulEggId: String?,
    private val prophecyEggId: String?,
) {
    val defaultEgg get() = this.defaultEggId?.let(::Snowflake)
    val edibleEgg get() = this.edibleEggId?.let(::Snowflake)
    val superfoodEgg get() = this.superfoodEggId?.let(::Snowflake)
    val medicalEgg get() = this.medicalEggId?.let(::Snowflake)
    val rocketFuelEgg get() = this.rocketFuelEggId?.let(::Snowflake)
    val superMaterialEgg get() = this.superMaterialEggId?.let(::Snowflake)
    val fusionEgg get() = this.fusionEggId?.let(::Snowflake)
    val quantumEgg get() = this.quantumEggId?.let(::Snowflake)
    val immortalityEgg get() = this.immortalityEggId?.let(::Snowflake)
    val tachyonEgg get() = this.tachyonEggId?.let(::Snowflake)
    val gravitonEgg get() = this.gravitonEggId?.let(::Snowflake)
    val dilithiumEgg get() = this.dilithiumEggId?.let(::Snowflake)
    val prodigyEgg get() = this.prodigyEggId?.let(::Snowflake)
    val terraformEgg get() = this.terraformEggId?.let(::Snowflake)
    val antimatterEgg get() = this.antimatterEggId?.let(::Snowflake)
    val darkMatterEgg get() = this.darkMatterEggId?.let(::Snowflake)
    val aiEgg get() = this.aiEggId?.let(::Snowflake)
    val nebulaEgg get() = this.nebulaEggId?.let(::Snowflake)
    val universeEgg get() = this.universeEggId?.let(::Snowflake)
    val enlightenmentEgg get() = this.enlightenmentEggId?.let(::Snowflake)
    val chocolateEgg get() = this.chocolateEggId?.let(::Snowflake)
    val easterEgg get() = this.easterEggId?.let(::Snowflake)
    val waterBalloonEgg get() = this.waterBalloonEggId?.let(::Snowflake)
    val fireworkEgg get() = this.fireworkEggId?.let(::Snowflake)
    val pumpkinEgg get() = this.pumpkinEggId?.let(::Snowflake)
    val goldEgg get() = this.goldEggId?.let(::Snowflake)
    val soulEgg get() = this.soulEggId?.let(::Snowflake)
    val prophecyEgg get() = this.prophecyEggId?.let(::Snowflake)
}

val config = when (envOrNull("ENVIRONMENT")) {
    "production" -> {
        logger.info { "Production environment: Loading remote config from ${env("CONFIG_URL")}" }
        Yaml.default.decodeFromString(
            deserializer = Config.serializer(),
            string = run {
                val (_, _, result) = Fuel.get(env("CONFIG_URL"))
                    .authentication()
                    .basic(env("CONFIG_USERNAME"), env("CONFIG_PASSWORD"))
                    .responseString()

                when (result) {
                    is Failure -> {
                        logger.error { result.getException() }
                        throw Exception("Failed to fetch configuration")
                    }
                    is Success -> {
                        result.get()
                    }
                }
            }
        )
    }
    else -> {
        logger.info { "Non-production environment: Loading local config" }
        Yaml.default.decodeFromStream(
            deserializer = Config.serializer(),
            File("eggbot.yaml").inputStream()
        )
    }
}
