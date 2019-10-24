package nl.pindab0ter.eggbot.jda

import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType.TEXT
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.joda.time.DateTime
import javax.annotation.Nonnull
import kotlin.random.Random


object EyeReaction : ListenerAdapter() {

    private val log = KotlinLogging.logger { }
    private val random = Random(DateTime.now().millisOfSecond)
    private const val EYES = "👀"

    override fun onMessageReceived(@Nonnull event: MessageReceivedEvent) {
        val roll = random.nextInt(9) // 1:10 chance

        if (event.author.id == "430901418925359126" && event.channelType == TEXT) {
            if (roll == 0) {
                log.trace { EYES }
                event.message.addReaction(EYES).queue()
            } else log.trace { "$roll ≠ 0" }
        }
    }
}
