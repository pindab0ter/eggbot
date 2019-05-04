package nl.pindab0ter.eggbot.jda

import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.ChannelType.TEXT
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import nl.pindab0ter.eggbot.EggBot


object CommandLogger : ListenerAdapter() {

    private val log = KotlinLogging.logger { }

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event?.author?.isBot == false &&
            listOfNotNull(
                commandClient.prefix,
                commandClient.altPrefix,
                "<@${EggBot.jdaClient.selfUser.id}>"
            ).any { event.message?.contentRaw?.startsWith(it) == true }
        ) log.trace {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            StringBuilder(event.author.name).apply {
                append(" (")
                when (event.channelType) {
                    PRIVATE -> append("DM")
                    TEXT -> append("#${event.channel.name}")
                }
                append("): ")
                append(event.message.contentDisplay)
            }
        }
    }
}
