package nl.pindab0ter.eggbot.helpers

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.findOrCreateById(id: ID, init: T.() -> Unit): T =
    findById(id) ?: new(id, init)