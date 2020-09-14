package nl.pindab0ter.eggbot.model.database

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.helpers.prophecyEggResearchLevel
import nl.pindab0ter.eggbot.helpers.soulEggResearchLevel
import nl.pindab0ter.eggbot.helpers.toDateTime
import nl.pindab0ter.eggbot.model.AuxBrain
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

    internal var soulEggsLong by Farmers.soulEggsLong
    internal var soulEggsDouble by Farmers.soulEggsDouble
    val soulEggs: BigDecimal
        get() = if (soulEggsLong > 0) BigDecimal(soulEggsLong) else BigDecimal(soulEggsDouble)
    var soulEggResearchLevel by Farmers.soulBonus
    var prophecyEggs by Farmers.prophecyEggs
    var prophecyEggResearchLevel by Farmers.prophecyBonus
    var prestiges by Farmers.prestiges
    var droneTakedowns by Farmers.droneTakedowns
    var eliteDroneTakedowns by Farmers.eliteDroneTakedowns
    var lastUpdated by Farmers.lastUpdated
    var coops by Coop via CoopFarmers

    val isActive: Boolean get() = discordUser.isActive

    val earningsBonus: BigDecimal
        get() = EarningsBonus(this).earningsBonus

    fun update() = AuxBrain.getFarmerBackup(inGameId)?.let { update(it) }
        ?: logger.warn { "Tried to update from backup but failed." }

    fun update(backup: Backup) {
        if (backup.clientVersion > EggBot.clientVersion) EggBot.clientVersion = backup.clientVersion
        if (backup.game == null || backup.stats == null) return logger.warn { "Tried to update from backup but failed." }
        if (!backup.userName.matches(Regex("""\[(android-)?unknown]"""))) inGameName = backup.userName
        prestiges = backup.stats.prestigeCount
        soulEggsLong = backup.game.soulEggsLong
        soulEggsDouble = backup.game.soulEggsDouble
        prophecyEggs = backup.game.prophecyEggs
        soulEggResearchLevel = backup.game.soulEggResearchLevel
        prophecyEggResearchLevel = backup.game.prophecyEggResearchLevel
        droneTakedowns = backup.stats.droneTakedowns
        eliteDroneTakedowns = backup.stats.droneTakedownsElite
        lastUpdated = backup.approxTime.toDateTime()
    }

    companion object : EntityClass<String, Farmer>(Farmers)
}
