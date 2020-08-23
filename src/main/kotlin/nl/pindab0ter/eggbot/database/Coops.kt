package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IntIdTable


object Coops : IntIdTable() {
    val name = text("name")
    val contract = text("contract_id")
    val role_id = text("role_id").nullable()

    init {
        this.index(true, name, contract)
    }
}
