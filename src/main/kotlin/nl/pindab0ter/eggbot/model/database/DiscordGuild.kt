package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.eggBot
import nl.pindab0ter.eggbot.guildSpecificCommands
import nl.pindab0ter.eggbot.helpers.kord
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID

class DiscordGuild(id: EntityID<String>) : Entity<String>(id) {
    val snowflake: Snowflake get() = Snowflake(this.id.value)
    var name: String by DiscordGuilds.name

    var createdAt by DiscordGuilds.createdAt
    var updatedAt by DiscordGuilds.updatedAt

    val discordUsers by DiscordUser referrersOn DiscordUsers.guildId

    fun asGuild() = runBlocking { kord.getGuild(snowflake) }

    companion object : EntityClass<String, DiscordGuild>(DiscordGuilds) {
        val logger = KotlinLogging.logger { }

        override fun new(id: String?, init: DiscordGuild.() -> Unit): DiscordGuild = super.new(id, init).also {
            runBlocking {
                logger.debug { "Refreshing extensions" }
                guildSpecificCommands.forEach { command ->
                    eggBot.removeExtension(command.name)
                    eggBot.addExtension(command)
                }
            }
        }

        fun findBySnowflake(snowflake: Snowflake): DiscordGuild? = DiscordGuild.find {
            DiscordGuilds.id eq snowflake.toString()
        }.firstOrNull()

        fun findOrCreate(discordGuild: dev.kord.core.entity.Guild): DiscordGuild = findBySnowflake(discordGuild.id) ?: new(discordGuild.id.toString()) {
            name = discordGuild.name
        }
    }
}

