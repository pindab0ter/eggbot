package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.SET_NULL


object Coops : IntIdTable() {
    val name = text("name")
    val contractId = text("contract_id")
    val roleId = text("role_id").nullable()
    val leaderId = reference("leader_id", Farmers, SET_NULL, SET_NULL).nullable()

    init {
        this.index(true, name, contractId)
    }
}
