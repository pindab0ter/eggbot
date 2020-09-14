package nl.pindab0ter.eggbot.jda

import net.dv8tion.jda.api.entities.ChannelType.PRIVATE
import net.dv8tion.jda.api.entities.ChannelType.TEXT
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import nl.pindab0ter.eggbot.EggBot.jdaClient
import org.apache.logging.log4j.kotlin.Logging
import javax.annotation.Nonnull


object IncomingCommand : ListenerAdapter(), Logging {

    override fun onMessageReceived(@Nonnull event: MessageReceivedEvent) {
        if (!event.author.isBot && listOfNotNull(
                commandClient.prefix,
                commandClient.altPrefix,
                jdaClient.selfUser.asMention
            ).any { event.message.contentRaw.startsWith(it) }
        ) logger.trace {
            buildString {
                append("${event.author.name} (")
                when (event.channelType) {
                    PRIVATE -> append("DM")
                    TEXT -> append("#${event.channel.name}")
                    else -> Unit
                }
                append("): ")
                append(event.message.contentDisplay)
            }
        }
    }
}
