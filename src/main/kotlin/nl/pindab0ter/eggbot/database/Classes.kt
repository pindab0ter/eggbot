package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID

class Farmer(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Farmer>(Farmers)

    var discordTag by Farmers.id
    var inGameName by Farmers.inGameName
//    var role by Farmers.role
}

class Contract(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Contract>(Contracts)

    val identifier: String get() = id.value
    var name by Contracts.title
    var description by Contracts.description
    var egg: EggInc.Egg by Contracts.egg
    var coopAllowed by Contracts.coopAllowed
    var maxCoopSize by Contracts.coopSize
    var expirationTime by Contracts.validUntil
    var lengthSeconds by Contracts.duration
}