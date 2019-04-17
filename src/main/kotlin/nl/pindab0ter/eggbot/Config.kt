package nl.pindab0ter.eggbot

import mu.KotlinLogging
import net.dv8tion.jda.core.entities.Game
import java.io.FileInputStream
import java.util.*

object Config {
    private val logger = KotlinLogging.logger { }

    private const val FILE_NAME = "config.properties"
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
    val devMode: Boolean

    init {
        Properties().apply {
            load(FileInputStream("config.properties"))

            botToken = getRequired(BOT_TOKEN)
            ownerId = getOptional(OWNER_ID, "0")
            prefix = getOptional(PREFIX, "!")
            helpWord = getOptional(HELP_WORD, "help")
            statusType = getOptional(STATUS_TYPE, "DEFAULT")
            statusText = getOptional(STATUS_TEXT, "").ifBlank { null }
            successEmoji = getOptional(SUCCESS_EMOJI, "👍")
            warningEmoji = getOptional(WARNING_EMOJI, "⚠️")
            errorEmoji = getOptional(ERROR_EMOJI, "🚫")
            coopName = getRequired(COOP_NAME)
            coopIncrementChar = getRequired(COOP_INCREMENT_CHAR).first()
            devMode = getOptional(DEVELOPMENT, "false") == "true"

            game = if (statusText != null) Game.of(
                when (statusType) {
                    "STREAMING" -> Game.GameType.STREAMING
                    "LISTENING" -> Game.GameType.LISTENING
                    "WATCHING" -> Game.GameType.WATCHING
                    else -> Game.GameType.DEFAULT
                }, statusText
            ) else null

            logger.info(
                """

                Bot token      : $botToken
                Owner ID       : $ownerId
                Prefix         : $prefix
                Help word      : $helpWord
                Status type    : $statusType
                Status text    : $statusText
                Success emoji  : $successEmoji
                Warning emoji  : $warningEmoji
                Error emoji    : $errorEmoji
                Co-op increment: $coopIncrementChar
                Co-op name     : $coopName
                Dev mode       : $devMode
                """.replaceIndent("\t")
            )
        }
    }

    private fun Properties.getRequired(key: String): String = getProperty(key).let {
        return when {
            !it.isNullOrBlank() -> it
            else -> throw PropertyNotFoundException("Could not load \"$key\" from \"$FILE_NAME\".")
        }
    }

    private fun Properties.getOptional(key: String, default: String): String = getProperty(key).let {
        return when (it) {
            null -> default
            else -> it
        }
    }
}
