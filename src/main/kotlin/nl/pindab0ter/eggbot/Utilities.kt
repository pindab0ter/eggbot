package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.entities.Message

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