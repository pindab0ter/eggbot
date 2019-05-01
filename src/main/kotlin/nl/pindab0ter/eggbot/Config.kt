package nl.pindab0ter.eggbot

import mu.KotlinLogging
import net.dv8tion.jda.core.entities.Game
import java.io.FileInputStream
import java.util.*

object Config {
    private val log = KotlinLogging.logger { }

    private const val FILE_NAME = "eggbot.properties"
    private const val BOT_TOKEN = "bot_token"
    private const val OWNER_ID = "owner_id"
    private const val PREFIX = "prefix"
    private const val HELP_WORD = "help_word"
    private const val STATUS_TYPE = "status_type"
    private const val STATUS_TEXT = "status_text"
    private const val SUCCESS_EMOJI = "success_emoji"
    private const val WARNING_EMOJI = "warning_emoji"
    private const val ERROR_EMOJI = "error_emoji"
    private const val COOP_NAME = "coop_name"
    private const val COOP_INCREMENT_CHAR = "coop_increment_char"

    private const val CHANNEL_BOT_COMMANDS = "channel.bot_commands"
    private const val CHANNEL_LEADER_BOARD_EARNINGS_BONUS = "channel.leader_board.earnings_bonus"
    private const val CHANNEL_LEADER_BOARD_SOUL_EGGS = "channel.leader_board.soul_eggs"
    private const val CHANNEL_LEADER_BOARD_PRESTIGES = "channel.leader_board.prestiges"
    private const val CHANNEL_LEADER_BOARD_DRONE_TAKEDOWNS = "channel.leader_board.drone_takedowns"
    private const val CHANNEL_LEADER_BOARD_ELITE_DRONE_TAKEDOWNS = "channel.leader_board.elite_drone_takedowns"

    private const val DEVELOPMENT = "dev_mode"

    val botToken: String
    val ownerId: String
    val prefix: String?
    val helpWord: String
    val game: Game?
    private val statusType: String
    private val statusText: String?
    val successEmoji: String
    val warningEmoji: String
    val errorEmoji: String
    val coopName: String
    val coopIncrementChar: Char

    val botCommandsChannel: String
    val earningsBonusLeaderBoardChannel: String
    val soulEggsLeaderBoardChannel: String
    val prestigesLeaderBoardChannel: String
    val droneTakedownsLeaderBoardChannel: String
    val eliteDroneTakedownsLeaderBoardChannel: String

    val devMode: Boolean

    init {
        Properties().apply {
            load(FileInputStream(FILE_NAME))

            botToken = getRequired(BOT_TOKEN)
            ownerId = getProperty(OWNER_ID, "0")
            prefix = getProperty(PREFIX, "!")
            helpWord = getProperty(HELP_WORD, "help")
            statusType = getProperty(STATUS_TYPE, "DEFAULT")
            statusText = getProperty(STATUS_TEXT)
            successEmoji = getProperty(SUCCESS_EMOJI, "👍")
            warningEmoji = getProperty(WARNING_EMOJI, "⚠️")
            errorEmoji = getProperty(ERROR_EMOJI, "🚫")
            coopName = getRequired(COOP_NAME)
            coopIncrementChar = getRequired(COOP_INCREMENT_CHAR).first()

            botCommandsChannel = getRequired(CHANNEL_BOT_COMMANDS)
            earningsBonusLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_EARNINGS_BONUS)
            soulEggsLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_SOUL_EGGS)
            prestigesLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_PRESTIGES)
            droneTakedownsLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_DRONE_TAKEDOWNS)
            eliteDroneTakedownsLeaderBoardChannel = getRequired(CHANNEL_LEADER_BOARD_ELITE_DRONE_TAKEDOWNS)

            devMode = getProperty(DEVELOPMENT, "false") == "true"

            game = if (statusText != null) Game.of(
                when (statusType) {
                    "STREAMING" -> Game.GameType.STREAMING
                    "LISTENING" -> Game.GameType.LISTENING
                    "WATCHING" -> Game.GameType.WATCHING
                    else -> Game.GameType.DEFAULT
                }, statusText
            ) else null

            log.info(
                """
                |Config loaded:
                |    Bot token      : $botToken
                |    Owner ID       : $ownerId
                |    Prefix         : $prefix
                |    Help word      : $helpWord
                |    Status type    : $statusType
                |    Status text    : $statusText
                |    Success emoji  : $successEmoji
                |    Warning emoji  : $warningEmoji
                |    Error emoji    : $errorEmoji
                |    Co-op increment: $coopIncrementChar
                |    Co-op name     : $coopName
                |    Dev mode       : $devMode
                """.replaceIndentByMargin("")
            )
        }
    }

    private fun Properties.getRequired(key: String): String = getProperty(key).let {
        return when {
            !it.isNullOrBlank() -> it
            else -> throw PropertyNotFoundException("Could not load \"$key\" from \"$FILE_NAME\".")
        }
    }
}
