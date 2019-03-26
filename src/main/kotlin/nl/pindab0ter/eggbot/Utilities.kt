package nl.pindab0ter.eggbot

import com.github.kittinunf.fuel.core.Body
import net.dv8tion.jda.core.entities.Message
import java.util.*

class MissingEnvironmentVariableException(message: String? = null) : Exception(message)

val Message.isCommand: Boolean
    get() = contentDisplay?.startsWith(prefix) == true && !author.isBot

val Message.command: String?
    get() = contentDisplay?.takeIf{ isCommand }
        ?.removePrefix(prefix)
        ?.split(' ')
        ?.first()

val Message.arguments: List<String>?
    get() = contentDisplay?.takeIf { isCommand }
        ?.removePrefix(prefix)
        ?.split(' ')
        ?.drop(1)

fun Body.base64Decoded(): ByteArray = Base64.getDecoder().decode(toByteArray())