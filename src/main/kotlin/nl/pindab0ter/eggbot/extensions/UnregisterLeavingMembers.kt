package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.annotation.KordPreview
import dev.kord.core.event.guild.MemberLeaveEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class UnregisterLeavingMembers : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = "CommandLoggerExtension"

    override suspend fun setup() {
        event<MemberLeaveEvent> {
            action {
                transaction {
                    logger.info { "User ${event.user.tag} left the server. Unregistering…" }
                    val discordUser = DiscordUser.findBy(event.user.id)

                    if (discordUser === null) {
                        logger.info { "Could not find registered user for ${event.user.tag}." }
                        return@transaction
                    }

                    val farmers = discordUser.farmers.map { farmer -> farmer.eggIncId to farmer.inGameName }
                    discordUser.delete()
                    commit()

                    logger.info { "Successfully removed ${farmers.joinToString { "${it.second} (${it.first})" }}" }
                }
            }
        }
    }
}
