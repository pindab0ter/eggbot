package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.entities.Message
import nl.pindab0ter.eggbot.commands.PREFIX

class MissingEnvironmentVariableException(message: String? = null) : Exception(message)

val Message.isCommand: Boolean
    get() = contentDisplay?.startsWith(PREFIX) == true && !author.isBot

val Message.command: String?
    get() = contentDisplay?.takeIf{ isCommand }
        ?.removePrefix(PREFIX)
        ?.split(' ')
        ?.first()

val Message.arguments: List<String>?
    get() = contentDisplay?.takeIf { isCommand }
        ?.removePrefix(PREFIX)
        ?.split(' ')
        ?.drop(1)
