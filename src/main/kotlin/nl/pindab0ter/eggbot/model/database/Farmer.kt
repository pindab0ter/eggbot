package nl.pindab0ter.eggbot.model.database

import ch.obermuhlner.math.big.BigDecimalMath
import com.auxbrain.ei.Backup
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.AuxBrain
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.div
import kotlin.math.ceil
import kotlin.math.roundToInt

class Farmer(id: EntityID<String>) : Entity<String>(id) {
    private val log = KotlinLogging.logger {}
    val inGameId: String get() = id.value
    var discordUser by DiscordUser referencedOn Farmers.discordId
    var inGameName by Farmers.inGameName

    internal var soulEggsLong by Farmers.soulEggsLong
    internal var soulEggsDouble by Farmers.soulEggsDouble
    val soulEggs: BigDecimal
        get() = if (soulEggsLong > 0) BigDecimal(soulEggsLong) else BigDecimal(soulEggsDouble)
    var prophecyEggs by Farmers.prophecyEggs
    var soulBonus by Farmers.soulBonus
    var prophecyBonus by Farmers.prophecyBonus
    var prestiges by Farmers.prestiges
    var droneTakedowns by Farmers.droneTakedowns
    var eliteDroneTakedowns by Farmers.eliteDroneTakedowns
    var lastUpdated by Farmers.lastUpdated
    var coops by Coop via CoopFarmers

    val isActive: Boolean get() = discordUser.isActive

    val role: String get() = earningsBonus.asFarmerRole()

    private val bonusPerProphecyEgg: BigDecimal
        get() = BigDecimal(1.05) + BigDecimal(0.01) * BigDecimal(prophecyBonus)
    private val bonusPerSoulEgg: BigDecimal
        get() {
            val soulEggBonus = BigDecimal(10 + soulBonus)
            return bonusPerProphecyEgg.pow(prophecyEggs.toInt()) * soulEggBonus
        }
    val seToNextRole: BigDecimal
        get() = earningsBonus.nextPowerOfTen()
            .minus(earningsBonus)
            .divide(bonusPerSoulEgg, RoundingMode.HALF_UP)
    val peToNextRole: Int
        get() = ceil(
            BigDecimalMath.log(earningsBonus.nextPowerOfTen() / earningsBonus, mathContext)
                .div(BigDecimalMath.log(bonusPerProphecyEgg, mathContext))
                .toDouble()
        ).roundToInt()
    val earningsBonus: BigDecimal get() = soulEggs * bonusPerSoulEgg

    fun update() = AuxBrain.getFarmerBackup(inGameId)?.let { update(it) }
        ?: log.warn { "Tried to update from backup but failed." }

    fun update(backup: Backup) {
        if (backup.clientVersion > EggBot.clientVersion) EggBot.clientVersion = backup.clientVersion
        if (backup.game == null || backup.stats == null) {
            log.warn { "Tried to update from backup but failed." }
            return
        }
        if (!backup.userName.matches(Regex("""\[(android-)?unknown]"""))) inGameName = backup.userName
        prestiges = backup.stats.prestigeCount
        soulEggsLong = backup.game.soulEggsLong
        soulEggsDouble = backup.game.soulEggsDouble
        prophecyEggs = backup.game.prophecyEggs
        soulBonus = backup.game.soulBonus
        prophecyBonus = backup.game.prophecyBonus
        droneTakedowns = backup.stats.droneTakedowns
        eliteDroneTakedowns = backup.stats.droneTakedownsElite
        lastUpdated = backup.approxTime.toDateTime()
    }

    companion object : EntityClass<String, Farmer>(Farmers)
}