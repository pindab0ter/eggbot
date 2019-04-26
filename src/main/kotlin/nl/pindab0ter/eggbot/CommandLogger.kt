package nl.pindab0ter.eggbot

import mu.KotlinLogging
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


object CommandLogger : ListenerAdapter() {

    private val log = KotlinLogging.logger { }

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event?.author?.isBot == false &&
            listOfNotNull(
                EggBot.commandClient.prefix,
                EggBot.commandClient.altPrefix,
                "<@${EggBot.jdaClient.selfUser.id}>"
            )
                .any { event.message?.contentRaw?.startsWith(it) == true }
        ) log.trace { "${event.author.name}: ${event.message.contentDisplay}" }
    }
}