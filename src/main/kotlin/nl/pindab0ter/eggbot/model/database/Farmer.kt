package nl.pindab0ter.eggbot.model.database

import com.auxbrain.ei.Backup
import dev.kord.common.entity.Snowflake
import mu.KotlinLogging
import nl.pindab0ter.eggbot.BASE_PROPHECY_EGG_RESEARCH_BONUS
import nl.pindab0ter.eggbot.BASE_SOUL_EGG_RESEARCH_BONUS
import nl.pindab0ter.eggbot.PROPHECY_EGG_RESEARCH_BONUS_PER_LEVEL
import nl.pindab0ter.eggbot.SOUL_EGG_RESEARCH_BONUS_PER_LEVEL
import nl.pindab0ter.eggbot.helpers.prophecyEggResearchLevel
import nl.pindab0ter.eggbot.helpers.soulEggResearchLevel
import nl.pindab0ter.eggbot.helpers.toDateTime
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.math.BigDecimal

class Farmer(id: EntityID<String>) : Entity<String>(id) {
    val eggIncId: String get() = this.id.value
    var discordUser by DiscordUser referencedOn Farmers.discordUserId
    private var _discordId by Farmers.discordUserId
    var discordId: Snowflake
        get() = Snowflake(_discordId.value)
        set(snowflake) {
            _discordId = EntityID(snowflake.toString(), Farmers)
        }
    var inGameName by Farmers.inGameName
    var coops by Coop via CoopFarmers

    private var _soulEggs by Farmers.soulEggs
    val soulEggs: BigDecimal
        get() = BigDecimal(_soulEggs)
    private var _soulEggResearchLevel by Farmers.soulBonus
    val soulEggResearchLevel
        get() = _soulEggResearchLevel.toBigDecimal()
    private var _prophecyEggs by Farmers.prophecyEggs
    val prophecyEggs
        get() = _prophecyEggs.toBigDecimal()
    private var _prophecyEggResearchLevel by Farmers.prophecyBonus
    val prophecyEggResearchLevel
        get() = _prophecyEggResearchLevel.toBigDecimal()

    private val soulEggBonus: BigDecimal
        get() = BASE_SOUL_EGG_RESEARCH_BONUS + (SOUL_EGG_RESEARCH_BONUS_PER_LEVEL * soulEggResearchLevel)
    private val prophecyEggBonus: BigDecimal
        get() = BigDecimal.ONE + BASE_PROPHECY_EGG_RESEARCH_BONUS + (PROPHECY_EGG_RESEARCH_BONUS_PER_LEVEL * prophecyEggResearchLevel)
    private val earningsBonusPerSoulEgg: BigDecimal
        get() = prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus * BigDecimal("100")
    val earningsBonus: BigDecimal
        get() = soulEggs * earningsBonusPerSoulEgg

    var prestiges by Farmers.prestiges
    var droneTakedowns by Farmers.droneTakedowns
    var eliteDroneTakedowns by Farmers.eliteDroneTakedowns

    var createdAt by Farmers.createdAt
    var updatedAt by Farmers.updatedAt

    val isActive: Boolean get() = discordUser.isActive

    fun update(backup: Backup) {
        if (backup.game == null || backup.stats == null) {
            logger.warn { "Tried to update from backup but failed." }
            return
        }

        inGameName = if (backup.userName.isNullOrBlank()) null else backup.userName
        _soulEggs = backup.game.soulEggs
        _prophecyEggs = backup.game.prophecyEggs
        _soulEggResearchLevel = backup.game.soulEggResearchLevel.toInt()
        _prophecyEggResearchLevel = backup.game.prophecyEggResearchLevel.toInt()
        prestiges = backup.stats.prestiges
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
                inGameName = if (backup.userName.isNullOrBlank()) null else backup.userName
                _soulEggs = backup.game.soulEggs
                _prophecyEggs = backup.game.prophecyEggs
                _soulEggResearchLevel = backup.game.soulEggResearchLevel.toInt()
                _prophecyEggResearchLevel = backup.game.prophecyEggResearchLevel.toInt()
                prestiges = backup.stats.prestiges
                droneTakedowns = backup.stats.droneTakedowns
                eliteDroneTakedowns = backup.stats.droneTakedownsElite
            }
        }
    }
}
