package nl.pindab0ter.eggbot.helpers

import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.Table.*

@DslMarker
annotation class TableMarker

inline fun table(init: Table.() -> Unit): List<String> =
    Table().also(init).render()

inline fun StringBuilder.appendTable(
    init: Table.() -> Unit,
): StringBuilder = also {
    Table().also(init).render().let { blocks ->
        blocks.forEach { block ->
            append(block)
            if (block != blocks.last()) appendLine()
        }
    }
}

inline fun List<Column>.renderRow(spacingChar: Char = ' ', transform: Column.() -> String) = StringBuilder().apply {
    this@renderRow.forEach { column ->
        append(spacingChar.repeat(column.leftPadding))
        append(column.transform())
        if (column != this@renderRow.last()) {
            append(spacingChar.repeat(column.rightPadding))
        }
    }
}
