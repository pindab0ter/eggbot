package nl.pindab0ter.eggbot.database

import nl.pindab0ter.eggbot.auxbrain.EggInc
import org.jetbrains.exposed.dao.*

class Farmer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Farmer>(Farmers)

    var discordTag by Farmers.discordTag
    var inGameName by Farmers.inGameName
    var role by Farmers.role
}

class Contract(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Contract>(Contracts)

    val identifier: String get() = id.value
    var name by Contracts.title
    var description by Contracts.description
    var egg: EggInc.Egg by Contracts.egg
    var coopAllowed by Contracts.coopAllowed
    var coopSize by Contracts.coopSize
    var validUntil by Contracts.validUntil
    var duration by Contracts.duration
}