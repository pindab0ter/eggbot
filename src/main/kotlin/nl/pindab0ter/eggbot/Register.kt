package nl.pindab0ter.eggbot

import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.exposed.sql.transactions.transaction

val log = logger("Register")

data class Registrant(
    val discordId: String,
    val discordTag: String,
    val inGameId: String,
    val inGameName: String,
)

fun registerUsers() = listOf(
    Registrant(
        "",
        "",
        "",
        ""
    )
).forEach(::register)

private fun register(registrant: Registrant) = transaction {
    val farmers = Farmer.all().toList()
    val backup = AuxBrain.getFarmerBackup(registrant.inGameId)

    // Check if the Discord user is already known, otherwise create a new user
    val discordUser: DiscordUser = DiscordUser.findById(registrant.discordId)
        ?: DiscordUser.new(registrant.discordId) {
            this.discordTag = registrant.discordTag
        }

    // Check if this Discord user hasn't already registered that in-game name
    if (discordUser.farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName }) return@transaction log.warn {
        "You are already registered with the in-game names: `${discordUser.farmers.joinToString("`, `") { it.inGameName }}`."
    }.also { rollback() }

    // Check if someone else hasn't already registered that in-game name
    if (farmers.any { it.inGameId == registrant.inGameId || it.inGameName == registrant.inGameName }) return@transaction log.warn {
        "Someone else has already registered the in-game name `${registrant.inGameName}`."
    }.also { rollback() }

    // Check if any back-up was found with the in-game ID
    if (backup?.game == null || backup.stats == null) return@transaction log.warn {
        """
        No account found with in-game ID `${registrant.inGameId}`. Did you enter your ID (not name!) correctly?
        """.trimIndent()
    }.also { rollback() }

    // Check if the in-game name matches with the in-game name belonging to the in-game ID's account
    if (!listOf(backup.userId,
            backup.eiUserId).contains(registrant.inGameId) || registrant.inGameName.lowercase() != backup.userName.lowercase()
    ) return@transaction log.warn {
        """
        The in-game name you entered (`${registrant.inGameName}`) does not match the name on record (`${backup.userName}`)
        """.trimIndent()
    }.also { rollback() }

    // Add the new in-game name
    val farmer = Farmer.new(discordUser, backup) ?: return@transaction log.warn {
        "Failed to save the new registration to the database. Please contact the bot maintainer."
    }

    // Finally, confirm the registration
    if (discordUser.farmers.filterNot { it.inGameId == registrant.inGameId }.none()) log.info {
        "You have been registered with the in-game name `${farmer.inGameName}`, welcome!"
    } else log.info {
        "You are now registered with the in-game name `${backup.userName}`, as well as `${
            discordUser.farmers
                .filterNot { it.inGameId == registrant.inGameId }
                .joinToString(" `, ` ") { it.inGameName }
        }`!"
    }
}
