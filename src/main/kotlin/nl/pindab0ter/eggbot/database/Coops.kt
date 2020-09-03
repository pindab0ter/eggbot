package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IntIdTable


object Coops : IntIdTable() {
    val name = text("name")
    val contractId = text("contract_id")
    val roleId = text("role_id").nullable()

    init {
        this.index(true, name, contractId)
    }
}
