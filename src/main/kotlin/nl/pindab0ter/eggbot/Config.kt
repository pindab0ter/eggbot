package nl.pindab0ter.eggbot

import java.io.FileInputStream
import java.util.*

object Config {
    private const val FILE_NAME = "config.properties"
    private const val BOT_TOKEN = "bot_token"
    private const val OWNER_ID = "owner_id"
    private const val COOP_NAME = "coop_name"
    private const val COOP_INCREMENT_CHAR = "coop_increment_char"
    private const val DEVELOPMENT = "dev_mode"

    val botToken: String
    val ownerId: String
    val coopName: String
    val coopIncrementChar: Char
    val devMode: Boolean

    init {
        Properties().apply {
            load(FileInputStream("config.properties"))

            botToken = getPropertyOrThrow(BOT_TOKEN)
            ownerId = getPropertyOrThrow(OWNER_ID)
            coopName = getPropertyOrThrow(COOP_NAME)
            coopIncrementChar = getPropertyOrThrow(COOP_INCREMENT_CHAR).first()
            devMode = getPropertyOrThrow(DEVELOPMENT) == "true"
        }
    }

    private fun Properties.getPropertyOrThrow(key: String): String = getProperty(key).let {
        return when {
            !it.isNullOrBlank() -> it
            else -> throw PropertyNotFoundException("Could not load \"$key\" from \"$FILE_NAME\".")
        }
    }
}
