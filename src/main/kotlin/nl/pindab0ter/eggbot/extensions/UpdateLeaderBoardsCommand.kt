package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permission.SendMessages
import dev.kord.common.entity.Permission.ViewChannel
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.getChannelOrNull
import nl.pindab0ter.eggbot.jobs.UpdateLeaderBoardsJob
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class UpdateLeaderBoardsCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() = config.servers.forEach { server ->
        ephemeralSlashCommand {
            name = "update-leader-boards"
            description = "Update the leader boards"
            guild(server.snowflake)

            check {
                hasRole(server.role.admin)
                passIf(event.interaction.user.id == config.botOwner)
                throwIfFailedWithMessage()

                failIf("There are no registered farmers.") { transaction(databases[server.name]) { Farmer.count() == 0L } }
                val guild = kord.getGuild(server.snowflake)
                server.channel.configuredLeaderBoards.forEach { (channelName, channelId) ->
                    val channel = guild?.getChannelOrNull(channelId)

                    if (channel == null) fail("The \"$channelName\" channel with ID $channelId was not found on server ${server.name}")
                    throwIfFailedWithMessage()

                    if (!channel!!.botHasPermissions(ViewChannel, SendMessages, Permission.ManageMessages, Permission.ReadMessageHistory)) {
                        fail(
                            "Bot does not have the required permissions for the \"$channelName\" channel with ID $channelId on server ${server.name}.\n" +
                                    "The required permissions are: \"View Channel\", \"Send Messages\", \"Manage Messages\" and \"Read Message History\""
                        )
                    }
                }
            }

            action {
                respond { content = "Started updating the leader boards." }
                UpdateLeaderBoardsJob().execute(null)
            }
        }
    }
}
