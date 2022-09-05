package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permission.SendMessages
import dev.kord.common.entity.Permission.ViewChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.getChannelOfOrNull
import nl.pindab0ter.eggbot.helpers.getChannelOrNull
import nl.pindab0ter.eggbot.helpers.onEachAsync
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.withProgressBar
import nl.pindab0ter.eggbot.view.leaderboardResponse
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class UpdateLeaderBoardsCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() = config.servers.forEach { server ->
        publicSlashCommand {
            name = "update-leader-boards"
            description = "Update the leader boards"
            guild(server.snowflake)

            check {
                failIf("There are no registered farmers.") {
                    transaction(databases[server.name]) { Farmer.count() == 0L }
                }

                throwIfFailedWithMessage()

                val guild = kord.getGuild(server.snowflake)
                server.configuredLeaderBoards.forEach { (channelName, channelId) ->
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
                // Update all farmers
                transaction(databases[server.name]) {
                    val oldFarmers = Farmer.all().toList()

                    runBlocking {
                        withProgressBar(
                            goal = oldFarmers.count(),
                            statusText = "Updating farmers…",
                            unit = "farmers",
                        ) {
                            oldFarmers.onEachAsync { farmer ->
                                AuxBrain.getFarmerBackup(farmer.eggIncId, databases[server.name])
                                increment()
                            }.toList()
                        }
                    }
                }

                // Retrieve updated farmers
                val farmers = transaction {
                    Farmer.all().toList()
                }

                withProgressBar(
                    goal = server.configuredLeaderBoards.count(),
                    statusText = "Updating leader boards…",
                    unit = "leader boards",
                ) {
                    server.configuredLeaderBoards
                        .mapValues { (_, channelSnowflake) ->
                            guild?.getChannelOfOrNull<TextChannel>(channelSnowflake)
                        }
                        .forEach { (category, textChannel) ->
                            textChannel?.withStrategy(EntitySupplyStrategy.rest)?.messages?.collect { message ->
                                message.delete("Updating leader boards")
                            }

                            guild?.leaderboardResponse(farmers, category, server = server)?.forEach { content ->
                                textChannel?.createMessage(content)
                            }

                            increment()
                        }
                }

                edit { content = "Updated all leader boards." }
            }
        }
    }
}
