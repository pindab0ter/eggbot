package nl.pindab0ter.eggbot.helpers

import com.auxbrain.ei.Egg
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.GuildChannel
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.eggsToEmotes

val kord: Kord get() = runBlocking { getKoin().get() }

fun GuildBehavior.emoteMention(egg: Egg): String? = when (val guild = runBlocking { asGuildOrNull() }) {
    null -> null
    else -> config.servers.find { server ->
        server.snowflake == guild.id
    }?.eggsToEmotes?.get(egg)?.let { eggEmojiSnowflake ->
        runBlocking { guild.getEmojiOrNull(eggEmojiSnowflake) }
    }?.mention
}

fun GuildBehavior?.getChannelOrNull(channelId: Snowflake?): GuildChannel? = when (channelId) {
    null -> null
    else -> runBlocking { this@getChannelOrNull?.asGuildOrNull()?.getChannelOrNull(channelId) }
}

fun <T> GuildBehavior?.getChannelOfOrNull(channelId: Snowflake?): T? = when (channelId) {
    null -> null
    else -> runBlocking { this@getChannelOfOrNull?.asGuildOrNull()?.getChannelOfOrNull(channelId) }
}

fun GuildBehavior?.getEmojiOrNull(emojiId: Snowflake?): GuildEmoji? = when (emojiId) {
    null -> null
    else -> runBlocking { this@getEmojiOrNull?.asGuildOrNull()?.getEmojiOrNull(emojiId) }
}

fun GuildBehavior?.getRoleOrNull(roleId: Snowflake?): Role? = when (roleId) {
    null -> null
    else -> runBlocking { this@getRoleOrNull?.asGuildOrNull()?.getRoleOrNull(roleId) }

}

fun GuildBehavior?.getMemberOrNull(userId: Snowflake?): Member? = when (userId) {
    null -> null
    else -> runBlocking { this@getMemberOrNull?.asGuildOrNull()?.getMemberOrNull(userId) }
}

fun GuildBehavior?.mentionUser(userId: Snowflake?): String? = when (userId) {
    null -> null
    else -> runBlocking { this@mentionUser?.getMemberOrNull(userId)?.mention ?: kord.getUser(userId)?.mention }
}
