package nl.pindab0ter.eggbot.model.database

import com.auxbrain.ei.Backup
import dev.kord.common.entity.Snowflake
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.prophecyEggResearchLevel
import nl.pindab0ter.eggbot.helpers.soulEggResearchLevel
import nl.pindab0ter.eggbot.helpers.toDateTime
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.EarningsBonus
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import java.math.BigDecimal

class Farmer(id: EntityID<String>) : Entity<String>(id) {
    val inGameId: String get() = this.id.value
    var discordUser by DiscordUser referencedOn Farmers.discordId
    private var _discordId by Farmers.discordId
    var discordId: Snowflake
        get() = Snowflake(_discordId.value)
        set(snowflake) {
            _discordId = EntityID(snowflake.toString(), Farmers)
        }
    var inGameName by Farmers.inGameName
    var coops by Coop via CoopFarmers

    var prestiges by Farmers.prestiges
    private var _soulEggs by Farmers.soulEggs
    val soulEggs: BigDecimal
        get() = BigDecimal(_soulEggs)
    var soulEggResearchLevel by Farmers.soulBonus
    var prophecyEggs by Farmers.prophecyEggs
    var prophecyEggResearchLevel by Farmers.prophecyBonus
    var droneTakedowns by Farmers.droneTakedowns
    var eliteDroneTakedowns by Farmers.eliteDroneTakedowns
    var createdAt by Farmers.createdAt
    var updatedAt by Farmers.updatedAt

    val isActive: Boolean get() = discordUser.isActive

    val earningsBonus: BigDecimal
        get() = EarningsBonus(this).earningsBonus

    fun update(backup: Backup) {
        if (backup.clientVersion > Config.clientVersion) {
            logger.info { "Updated to client version ${backup.clientVersion}." }
            Config.clientVersion = backup.clientVersion
        }

        if (backup.game == null || backup.stats == null) {
            logger.warn { "Tried to update from backup but failed." }
            return
        }

        if (!backup.userName.matches(Regex("""\[(android-)?unknown]"""))) {
            inGameName = backup.userName
        } else {
            logger.warn { "Found an invalid in-game name: ${backup.userName} for {${backup.userId}" }
        }

        prestiges = backup.stats.prestigeCount
        _soulEggs = backup.game.soulEggs
        prophecyEggs = backup.game.prophecyEggs
        soulEggResearchLevel = backup.game.soulEggResearchLevel
        prophecyEggResearchLevel = backup.game.prophecyEggResearchLevel
        droneTakedowns = backup.stats.droneTakedowns
        eliteDroneTakedowns = backup.stats.droneTakedownsElite
        updatedAt = backup.approxTime.toDateTime()
    }

    companion object : EntityClass<String, Farmer>(Farmers) {
        val logger = KotlinLogging.logger { }

        fun new(discordUser: DiscordUser, backup: Backup): Farmer? {
            if (backup.game == null || backup.stats == null) {
                logger.warn { "Tried to register from backup but failed." }
                return null
            }

            return Farmer.new(backup.eiUserId.ifBlank { backup.userId }) {
                this.discordUser = discordUser
                if (backup.userName.isNotBlank()) inGameName = backup.userName
                prestiges = backup.stats.prestigeCount
                _soulEggs = backup.game.soulEggs
                prophecyEggs = backup.game.prophecyEggs
                soulEggResearchLevel = backup.game.soulEggResearchLevel
                prophecyEggResearchLevel = backup.game.prophecyEggResearchLevel
                droneTakedowns = backup.stats.droneTakedowns
                eliteDroneTakedowns = backup.stats.droneTakedownsElite
                updatedAt = backup.approxTime.toDateTime()
            }
        }
    }
}
