package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Activity
import java.io.FileInputStream
import java.util.*

object Config {
    private val log = KotlinLogging.logger { }

    private const val FILE_NAME = "eggbot.properties"
    private const val BOT_TOKEN = "bot_token"
    private const val CLIENT_VERSION = "client_version"
    private const val OWNER_ID = "owner_id"
    private const val PREFIX = "prefix"
    private const val HELP_WORD = "help_word"
    private const val STATUS_TYPE = "status_type"
    private const val STATUS_TEXT = "status_text"
    private const val EMOJI_SUCCESS = "emoji.success"
    private const val EMOJI_WARNING = "emoji.warning"
    private const val EMOJI_ERROR = "emoji.error"
    private const val COOP_NAME = "coop_name"
    private const val COOP_INCREMENT_CHAR = "coop_increment_char"

    private const val CHANNEL_BOT_COMMANDS = "channel.bot_commands"
    private const val CHANNEL_LEADER_BOARD_EARNINGS_BONUS = "channel.leader_board.earnings_bonus"
    private const val CHANNEL_LEADER_BOARD_SOUL_EGGS = "channel.leader_board.soul_eggs"
    private const val CHANNEL_LEADER_BOARD_PRESTIGES = "channel.leader_board.prestiges"
    private const val CHANNEL_LEADER_BOARD_DRONE_TAKEDOWNS = "channel.leader_board.drone_takedowns"
    private const val CHANNEL_LEADER_BOARD_ELITE_DRONE_TAKEDOWNS = "channel.leader_board.elite_drone_takedowns"

    private const val ROLE_ADMIN = "role.admin"

    private const val EMOTE_DEFAULT = "emote.default"
    private const val EMOTE_EDIBLE = "emote.edible"
    private const val EMOTE_SUPERFOOD = "emote.superfood"
    private const val EMOTE_MEDICAL = "emote.medical"
    private const val EMOTE_ROCKET_FUEL = "emote.rocket_fuel"
    private const val EMOTE_SUPER_MATERIAL = "emote.super_material"
    private const val EMOTE_FUSION = "emote.fusion"
    private const val EMOTE_QUANTUM = "emote.quantum"
    private const val EMOTE_IMMORTALITY = "emote.immortality"
    private const val EMOTE_TACHYON = "emote.tachyon"
    private const val EMOTE_GRAVITON = "emote.graviton"
    private const val EMOTE_DILITHIUM = "emote.dilithium"
    private const val EMOTE_PRODIGY = "emote.prodigy"
    private const val EMOTE_TERRAFORM = "emote.terraform"
    private const val EMOTE_ANTIMATTER = "emote.antimatter"
    private const val EMOTE_DARK_MATTER = "emote.dark_matter"
    private const val EMOTE_AI = "emote.ai"
    private const val EMOTE_NEBULA = "emote.nebula"
    private const val EMOTE_UNIVERSE = "emote.universe"
    private const val EMOTE_ENLIGHTENMENT = "emote.enlightenment"
    private const val EMOTE_CHOCOLATE = "emote.chocolate"
    private const val EMOTE_EASTER = "emote.easter"
    private const val EMOTE_WATER_BALLOON = "emote.water_balloon"
    private const val EMOTE_FIREWORK = "emote.firework"
    private const val EMOTE_PUMPKIN = "emote.pumpkin"
    private const val EMOTE_GOLD = "emote.gold"
    private const val EMOTE_SOUL = "emote.soul"
    private const val EMOTE_PROPHECY = "emote.prophecy"

    private const val DEVELOPMENT = "dev_mode"

    val botToken: String
    val clientVersion: Int
    val ownerId: String
    val prefix: String?
    val helpWord: String
    val activity: Activity?
    private val statusType: String
    private val statusText: String?
    val emojiSuccess: String
    val emojiWarning: String
    val emojiError: String
    val coopName: String
    val coopIncrementChar: Char

    val botCommandsChannel: String
    val earningsBonusLeaderBoardChannel: String
    val soulEggsLeaderBoardChannel: String
    val prestigesLeaderBoardChannel: String
    val droneTakedownsLeaderBoardChannel: String
    val eliteDroneTakedownsLeaderBoardChannel: String

    val adminRole: String?

    val eggEmojiIds: Map<EggInc.Egg, String?>

    val emoteGoldenEgg: String?
    val emoteSoulEgg: String?
    val emoteProphecyEgg: String?

    val devMode: Boolean

    init {
        Properties().apply {
            load(FileInputStream(FILE_NAME))

            botToken = getRequired(BOT_TOKEN)
            clientVersion = getOptional(CLIENT_VERSION, "0").toInt()
            ownerId = getOptional(OWNER_ID, "0")
            prefix = getOptional(PREFIX, "!")
            helpWord = getOptional(HELP_WORD, "help")
            statusType = getOptional(STATUS_TYPE, "DEFAULT")
            statusText = getOptional(STATUS_TEXT)
            emojiSuccess = getOptional(EMOJI_SUCCESS, "👍")
            emojiWarning = getOptional(EMOJI_WARNING, "⚠️")
            emojiError = getOptional(EMOJI_ERROR, "🚫")
            coopName = getRequired(COOP_NAME)
            coopIncrementChar = getRequired(COOP_INCREMENT_CHAR).first()

            botCommandsChannel = getRequired(CHANNEL_BOT_COMMANDS)
            earningsBonusLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_EARNINGS_BONUS)
            soulEggsLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_SOUL_EGGS)
            prestigesLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_PRESTIGES)
            droneTakedownsLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_DRONE_TAKEDOWNS)
            eliteDroneTakedownsLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_ELITE_DRONE_TAKEDOWNS)

            adminRole = getOptional(ROLE_ADMIN)

            // @formatter:off
            eggEmojiIds = mapOf (
                EggInc.Egg.UNKNOWN_EGG    to getOptional(EMOTE_DEFAULT),
                EggInc.Egg.EDIBLE         to getOptional(EMOTE_EDIBLE),
                EggInc.Egg.SUPERFOOD      to getOptional(EMOTE_SUPERFOOD),
                EggInc.Egg.MEDICAL        to getOptional(EMOTE_MEDICAL),
                EggInc.Egg.ROCKET_FUEL    to getOptional(EMOTE_ROCKET_FUEL),
                EggInc.Egg.SUPER_MATERIAL to getOptional(EMOTE_SUPER_MATERIAL),
                EggInc.Egg.FUSION         to getOptional(EMOTE_FUSION),
                EggInc.Egg.QUANTUM        to getOptional(EMOTE_QUANTUM),
                EggInc.Egg.IMMORTALITY    to getOptional(EMOTE_IMMORTALITY),
                EggInc.Egg.TACHYON        to getOptional(EMOTE_TACHYON),
                EggInc.Egg.GRAVITON       to getOptional(EMOTE_GRAVITON),
                EggInc.Egg.DILITHIUM      to getOptional(EMOTE_DILITHIUM),
                EggInc.Egg.PRODIGY        to getOptional(EMOTE_PRODIGY),
                EggInc.Egg.TERRAFORM      to getOptional(EMOTE_TERRAFORM),
                EggInc.Egg.ANTIMATTER     to getOptional(EMOTE_ANTIMATTER),
                EggInc.Egg.DARK_MATTER    to getOptional(EMOTE_DARK_MATTER),
                EggInc.Egg.AI             to getOptional(EMOTE_AI),
                EggInc.Egg.NEBULA         to getOptional(EMOTE_NEBULA),
                EggInc.Egg.UNIVERSE       to getOptional(EMOTE_UNIVERSE),
                EggInc.Egg.ENLIGHTENMENT  to getOptional(EMOTE_ENLIGHTENMENT),
                EggInc.Egg.CHOCOLATE      to getOptional(EMOTE_CHOCOLATE),
                EggInc.Egg.EASTER         to getOptional(EMOTE_EASTER),
                EggInc.Egg.WATER_BALLOON  to getOptional(EMOTE_WATER_BALLOON),
                EggInc.Egg.FIREWORK       to getOptional(EMOTE_FIREWORK),
                EggInc.Egg.PUMPKIN        to getOptional(EMOTE_PUMPKIN)
            )
            // @formatter:on

            emoteGoldenEgg = getOptional(EMOTE_GOLD)
            emoteSoulEgg = getOptional(EMOTE_SOUL)
            emoteProphecyEgg = getOptional(EMOTE_PROPHECY)

            devMode = getOptional(DEVELOPMENT, "false") == "true"

            activity = if (statusText != null) Activity.of(
                when (statusType) {
                    "STREAMING" -> Activity.ActivityType.STREAMING
                    "LISTENING" -> Activity.ActivityType.LISTENING
                    "WATCHING" -> Activity.ActivityType.WATCHING
                    else -> Activity.ActivityType.DEFAULT
                }, statusText
            ) else null

            log.info("Config loaded")
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
