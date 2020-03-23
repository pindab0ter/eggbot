package nl.pindab0ter.eggbot.jda

import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType.PRIVATE
import net.dv8tion.jda.api.entities.ChannelType.TEXT
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import nl.pindab0ter.eggbot.EggBot.jdaClient
import javax.annotation.Nonnull


object CommandLogger : ListenerAdapter() {

    private val log = KotlinLogging.logger { }

    override fun onMessageReceived(@Nonnull event: MessageReceivedEvent) {
        if (!event.author.isBot && listOfNotNull(
                commandClient.prefix,
                commandClient.altPrefix,
                jdaClient.selfUser.asMention
            ).any { event.message.contentRaw.startsWith(it) }
        ) log.trace {
            StringBuilder(event.author.name).apply {
                append(" (")
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
