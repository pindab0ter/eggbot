package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object CoopFarmers : Table() {
    val farmer = reference("farmer", Farmers, ReferenceOption.SET_NULL, ReferenceOption.CASCADE)
    val coop = reference("coop", Coops, ReferenceOption.SET_NULL, ReferenceOption.CASCADE)

    init {
        this.index(true, farmer, coop)
    }
}
