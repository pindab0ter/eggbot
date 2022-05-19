package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.config

@KordPreview
class TestCommand : Extension() {
    override val name: String = javaClass.simpleName

    override suspend fun setup() = config.servers.forEach { server ->
        ephemeralSlashCommand {
            name = "test"
            description = "Used for testing purposes"
            guild(server.snowflake)

            action {
                respond { content = "Test" }
            }
        }
    }
}
