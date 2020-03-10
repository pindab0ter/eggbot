package nl.pindab0ter.eggbot.utilities

import nl.pindab0ter.eggbot.utilities.Aligned.*
import kotlin.text.StringBuilder

@DslMarker
annotation class TableMarker

class Table(
    val columns: List<Column>
) {
    val amountOfRows: Int = columns.first().cells.size

    init {
        require(columns.all { it.size == amountOfRows }) { "All columns must be of equal size" }
    }

    override fun toString(): String = StringBuilder().apply {

        // Calculate spacing:

        val spacingColumns = columns.zipWithNext { left, right ->
            if (left is ValueColumn && left.aligned == LEFT &&
                right is ValueColumn && right.aligned == RIGHT
            ) {
                val widest = left.cells.zip(right.cells).map { (a, b) -> a.length + b.length }.max()!!

                // TODO: Move to SpacingColumn constructor/init
                SpacingColumn(left.cells.zip(right.cells) { a: String, b: String ->
                    widest - (a.length + b.length)
                })

            } else {
                SpacingColumn(length, 1)
            }
        }

        val spacedColumns = columns.interleave(spacingColumns)

        (0 until amountOfRows).forEach { row ->
            spacedColumns.forEach { column ->
                append(column.cells[row])
            }
            appendln()
        }
    }.toString()
}

abstract class Column(
    val header: String,
    val cells: List<String>
) {
    init {
        require(cells.isNotEmpty()) { "Columns must have data" }
    }

    val size: Int = cells.size
    val widths: List<Int> = cells.map(String::length)
    val widest: Int = widths.max()!!
}

open class DividerColumn(
    divider: String = "│",
    size: Int
) : Column(divider, List(size) { divider })

class SpacingColumn : Column {
    constructor(size: Int, width: Int) : super(" ", List(size) { " ".repeat(width) })
    constructor(widths: List<Int>) : super(" ", widths.map { width -> " ".repeat(width) })
}

enum class Aligned {
    LEFT, RIGHT
}

class ValueColumn(
    header: String,
    values: List<Any>,
    val aligned: Aligned = LEFT
) : Column(header, values.map(Any::toString))

// fun table(init: Table.() -> Unit): Table = Table().also(init)
