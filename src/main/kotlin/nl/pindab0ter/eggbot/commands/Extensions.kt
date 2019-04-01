package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent

val CommandEvent.arguments: List<String>
    get() = if (args.isBlank()) emptyList() else args.split(' ')
