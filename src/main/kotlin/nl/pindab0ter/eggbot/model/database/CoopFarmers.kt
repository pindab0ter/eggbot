package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table


object CoopFarmers : Table() {
    override val tableName = "coop_farmers"
    val farmer = reference("farmer", Farmers, CASCADE, CASCADE)
    val coop = reference("coop", Coops, CASCADE, CASCADE)

    init {
        index(true, farmer, coop)
    }
}
