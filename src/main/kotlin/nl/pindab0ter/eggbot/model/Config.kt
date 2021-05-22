package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.Egg
import net.dv8tion.jda.api.entities.Activity
import org.apache.logging.log4j.kotlin.Logging
import java.io.FileInputStream
import java.util.*

object Config : Logging {
    private const val FILE_NAME = "eggbot.properties"

    val botToken: String
    val prefix: String
    val helpWord: String
    val emojiSuccess: String
    val emojiWarning: String
    val emojiError: String
    val activity: Activity?
    private val statusType: String
    private val statusText: String?
    val clientVersion: Int
    val devMode: Boolean

    // AuxBrain IDs
    val userId: String
    val deviceId: String

    // Discord IDs
    val botOwnerId: String
    val guildId: Long
    val adminRoleId: String

    val botCommandsChannelId: String
    val earningsBonusLeaderBoardChannelId: String
    val soulEggsLeaderBoardChannelId: String
    val prestigesLeaderBoardChannelId: String
    val dronesLeaderBoardChannelId: String
    val eliteDronesLeaderBoardChannelId: String

    val eggsToEmoteIds: Map<Egg, String?>
    val emoteGoldenEggId: String?
    val emoteSoulEggId: String?
    val emoteProphecyEggId: String?

    init {
        Properties().apply {
            load(FileInputStream(FILE_NAME))

            botToken = getRequired("bot_token")
            prefix = getOptional("prefix", "!")
            helpWord = getOptional("help_word", "help")
            emojiSuccess = getOptional("emoji.success", "👍")
            emojiWarning = getOptional("emoji.warning", "⚠️")
            emojiError = getOptional("emoji.error", "🚫")
            statusType = getOptional("status_type", "DEFAULT")
            statusText = getOptional("status_text")
            activity = if (statusText != null) Activity.of(
                when (statusType) {
                    "STREAMING" -> Activity.ActivityType.STREAMING
                    "LISTENING" -> Activity.ActivityType.LISTENING
                    "WATCHING" -> Activity.ActivityType.WATCHING
                    else -> Activity.ActivityType.DEFAULT
                }, statusText
            ) else null
            clientVersion = getOptional("client_version", "0").toInt()
            devMode = getOptional("dev_mode", "false") == "true"

            userId = getRequired("user_id")
            deviceId = getRequired("device_id")

            botOwnerId = getOptional("bot_owner_id", "0")
            guildId = getRequired("guild_id").toLong()
            adminRoleId = getRequired("role.admin_id")

            botCommandsChannelId = getRequired("channel.bot_commands")
            earningsBonusLeaderBoardChannelId = getRequired("channel.leader_board.earnings_bonus")
            soulEggsLeaderBoardChannelId = getRequired("channel.leader_board.soul_eggs")
            prestigesLeaderBoardChannelId = getRequired("channel.leader_board.prestiges")
            dronesLeaderBoardChannelId = getRequired("channel.leader_board.drone_takedowns")
            eliteDronesLeaderBoardChannelId = getRequired("channel.leader_board.elite_drone_takedowns")

            // @formatter:off
                eggsToEmoteIds = mapOf (
                    Egg.UNKNOWN_EGG    to getOptional("emote.default"),
                    Egg.EDIBLE         to getOptional("emote.edible"),
                    Egg.SUPERFOOD      to getOptional("emote.superfood"),
                    Egg.MEDICAL        to getOptional("emote.medical"),
                    Egg.ROCKET_FUEL    to getOptional("emote.rocket_fuel"),
                    Egg.SUPER_MATERIAL to getOptional("emote.super_material"),
                    Egg.FUSION         to getOptional("emote.fusion"),
                    Egg.QUANTUM        to getOptional("emote.quantum"),
                    Egg.IMMORTALITY    to getOptional("emote.immortality"),
                    Egg.TACHYON        to getOptional("emote.tachyon"),
                    Egg.GRAVITON       to getOptional("emote.graviton"),
                    Egg.DILITHIUM      to getOptional("emote.dilithium"),
                    Egg.PRODIGY        to getOptional("emote.prodigy"),
                    Egg.TERRAFORM      to getOptional("emote.terraform"),
                    Egg.ANTIMATTER     to getOptional("emote.antimatter"),
                    Egg.DARK_MATTER    to getOptional("emote.dark_matter"),
                    Egg.AI             to getOptional("emote.ai"),
                    Egg.NEBULA         to getOptional("emote.nebula"),
                    Egg.UNIVERSE       to getOptional("emote.universe"),
                    Egg.ENLIGHTENMENT  to getOptional("emote.enlightenment"),
                    Egg.CHOCOLATE      to getOptional("emote.chocolate"),
                    Egg.EASTER         to getOptional("emote.easter"),
                    Egg.WATER_BALLOON  to getOptional("emote.water_balloon"),
                    Egg.FIREWORK       to getOptional("emote.firework"),
                    Egg.PUMPKIN        to getOptional("emote.pumpkin")
                )
                // @formatter:on

            emoteGoldenEggId = getOptional("emote.gold")
            emoteSoulEggId = getOptional("emote.soul")
            emoteProphecyEggId = getOptional("emote.prophecy")

            logger.info("Config loaded")
        }
    }

    private fun Properties.getRequired(key: String): String = getProperty(key).let {
        return when {
            !it.isNullOrBlank() -> it
            else -> throw PropertyNotFoundException("Could not load \"$key\" from \"$FILE_NAME\".")
        }
    }

    private fun Properties.getOptional(key: String): String? = getProperty(key).let {
        return when {
            !it.isNullOrBlank() -> it
            else -> null
        }
    }

    private fun Properties.getOptional(key: String, default: String): String = getProperty(key).let {
        return when {
            !it.isNullOrBlank() -> it
            else -> default
        }
    }

    class PropertyNotFoundException(override val message: String?) : Exception(message)
}
