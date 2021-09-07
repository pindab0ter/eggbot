package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table


object CoopFarmers : Table() {
    val farmer = reference("farmer", Farmers, CASCADE, CASCADE)
    val coop = reference("coop", Coops, CASCADE, CASCADE)

    init {
        this.index(true, farmer, coop)
    }
}
