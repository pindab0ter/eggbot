package nl.pindab0ter.eggbot.jda

import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType.PRIVATE
import net.dv8tion.jda.api.entities.ChannelType.TEXT
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import nl.pindab0ter.eggbot.EggBot
import javax.annotation.Nonnull


object CommandLogger : ListenerAdapter() {

    private val log = KotlinLogging.logger { }

    override fun onMessageReceived(@Nonnull event: MessageReceivedEvent) {
        if (!event.author.isBot && listOfNotNull(
                commandClient.prefix,
                commandClient.altPrefix,
                "<@${EggBot.jdaClient.selfUser.id}>"
            ).any { event.message.contentRaw.startsWith(it) == true }
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
