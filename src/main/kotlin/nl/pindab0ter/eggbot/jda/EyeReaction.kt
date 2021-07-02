package nl.pindab0ter.eggbot.jda

import net.dv8tion.jda.api.entities.ChannelType.TEXT
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.joda.time.DateTime
import kotlin.random.Random


object EyeReaction : ListenerAdapter() {
    private val random = Random(DateTime.now().millisOfSecond)
    private const val EYES = "👀"

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.id == "860368140470321213"
            && event.channelType == TEXT
            && random.nextInt(9) == 0 // 1:10 chance
        ) {
            event.message.addReaction(EYES).queue()
        }
    }
}