package nl.pindab0ter.eggbot.model.database

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.helpers.prophecyEggResearchLevel
import nl.pindab0ter.eggbot.helpers.soulEggResearchLevel
import nl.pindab0ter.eggbot.helpers.toDateTime
import nl.pindab0ter.eggbot.model.EarningsBonus
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import java.math.BigDecimal

class Farmer(id: EntityID<String>) : Entity<String>(id), Logging {
    val inGameId: String get() = id.value
    var discordUser by DiscordUser referencedOn Farmers.discordId
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
    var lastUpdated by Farmers.lastUpdated

    val isActive: Boolean get() = discordUser.isActive

    val earningsBonus: BigDecimal
        get() = EarningsBonus(this).earningsBonus

    fun update(backup: Backup) {
        if (backup.clientVersion > EggBot.clientVersion) EggBot.clientVersion = backup.clientVersion
        if (backup.game == null || backup.stats == null) return logger.warn { "Tried to update from backup but failed." }
        if (!backup.userName.matches(Regex("""\[(android-)?unknown]"""))) inGameName = backup.userName
        prestiges = backup.stats.prestigeCount
        _soulEggs = backup.game.soulEggs
        prophecyEggs = backup.game.prophecyEggs
        soulEggResearchLevel = backup.game.soulEggResearchLevel
        prophecyEggResearchLevel = backup.game.prophecyEggResearchLevel
        droneTakedowns = backup.stats.droneTakedowns
        eliteDroneTakedowns = backup.stats.droneTakedownsElite
        lastUpdated = backup.approxTime.toDateTime()
    }

    companion object : EntityClass<String, Farmer>(Farmers), Logging {
        fun new(discordUser: DiscordUser, backup: Backup): Farmer? {
            if (backup.game == null || backup.stats == null) return null.also { logger.warn { "Tried to register from backup but failed." } }

            return Farmer.new(if (backup.eiUserId.isNotBlank()) backup.eiUserId else backup.userId) {
                this.discordUser = discordUser
                if (backup.userName.isNotBlank()) inGameName = backup.userName
                prestiges = backup.stats.prestigeCount
                _soulEggs = backup.game.soulEggs
                prophecyEggs = backup.game.prophecyEggs
                soulEggResearchLevel = backup.game.soulEggResearchLevel
                prophecyEggResearchLevel = backup.game.prophecyEggResearchLevel
                droneTakedowns = backup.stats.droneTakedowns
                eliteDroneTakedowns = backup.stats.droneTakedownsElite
                lastUpdated = backup.approxTime.toDateTime()
            }
        }
    }
}
