package nl.pindab0ter.eggbot.utilities

import nl.pindab0ter.eggbot.utilities.Table.*
import nl.pindab0ter.eggbot.utilities.Table.AlignedColumn.Alignment.*

@DslMarker
annotation class TableMarker

@TableMarker
@Suppress("SuspiciousVarProperty")
class Table {

    // region Properties

    private val columns: MutableList<Column> = mutableListOf()
    private val alignedColumns: List<AlignedColumn> get() = columns.filterIsInstance<AlignedColumn>()
    private val amountOfRows: Int get() = (columns.first { it is ValueColumn } as? ValueColumn)?.cells?.size ?: 0
    var title: String? = null
    var displayHeader: Boolean = true

    // endregion

    // region Column classes

    @TableMarker
    abstract class Column {
        open var header: String = ""
        open var cells: List<String> = emptyList()
        var leftPadding = 0
        var rightPadding = 0
    }

    abstract class AlignedColumn : Column() {
        enum class Alignment { LEFT, RIGHT }

        var alignment: Alignment = LEFT
    }

    class ValueColumn : AlignedColumn()

    class EmojiColumn : Column() {
        var borderEmoji = "➖"
    }

    inner class DividerColumn : Column() {
        var border: Char = '│'
        var intersection: Char = '╪'
        private val _cells: List<String> by lazy { List(this@Table.amountOfRows) { "$border" } }
        override var cells: List<String> = emptyList()
            get() = _cells
    }

    inner class IncrementColumn : AlignedColumn() {
        init {
            alignment = RIGHT
        }

        private val _header: String by lazy { "${'#'.repeat(this@Table.amountOfRows.toString().length)}$suffix" }
        override var header: String = ""
            get() = _header
        private val _cells: List<String> by lazy { List(this@Table.amountOfRows) { index -> "${index + 1}$suffix" } }
        override var cells: List<String> = emptyList()
            get() = _cells
        var suffix: String = ""
    }

    private inner class SpacingColumn(left: Column?, right: Column?) : Column() {
        init {
            val (header, cells) = when {
                left is AlignedColumn && left.alignment == LEFT && right is AlignedColumn && right.alignment == RIGHT ->
                    left.cellLengths.zip(right.cellLengths).let { rows ->
                        val widestPair = rows.map { (a, b) -> a + b }.max()!!
                        ' '.repeat(widestPair - (left.headerLength + right.headerLength)) to rows.map { (a, b) ->
                            ' '.repeat(widestPair - (a + b))
                        }
                    }
                left is AlignedColumn && left.alignment == LEFT ->
                    left.longest.let { longest ->
                        ' '.repeat(longest - left.headerLength) to left.cellLengths.map { width ->
                            ' '.repeat(longest - width)
                        }
                    }
                right is AlignedColumn && right.alignment == RIGHT ->
                    right.longest.let { longest ->
                        ' '.repeat(longest - right.headerLength) to right.cellLengths.map { width ->
                            ' '.repeat(longest - width)
                        }
                    }
                else -> "" to List(this@Table.amountOfRows) { "" }
            }

            this.header = header
            this.cells = cells
        }
    }

    // endregion

    // region Builder functions

    private fun <T : Column> initColumn(column: T, init: T.() -> Unit): T {
        column.init()
        columns += column
        return column
    }

    fun column(init: ValueColumn.() -> Unit): ValueColumn = initColumn(ValueColumn(), init)

    fun emojiColumn(init: EmojiColumn.() -> Unit): EmojiColumn = initColumn(EmojiColumn(), init)

    fun incrementColumn(suffix: String = ""): IncrementColumn = initColumn(IncrementColumn(), {
        this.suffix = suffix
    })

    fun divider(border: Char = '│', intersection: Char = '╪'): DividerColumn = initColumn(DividerColumn(), {
        this.border = border
        this.intersection = intersection
    })

    // endregion

    companion object {
        // Moved here to prevent visibility from constructor lambda
        val Column.headerLength: Int get() = header.length
        val Column.cellLengths: List<Int> get() = cells.map { cell -> cell.length }
        val Column.longest: Int get() = (cells.map { cell -> cell.length }).plus(header.length).max() ?: 0
    }

    override fun toString(): String = StringBuilder().apply {
        require(alignedColumns.filterIsInstance<ValueColumn>().isNotEmpty()) { "Table must have ValueColumns" }
        require(columns.all { it.cells.size == amountOfRows }) { "All columns must be of equal size" }

        // Add SpacingColumns between adjacent left and right aligned columns and before and after the last column
        val spacedColumns: List<Column> = columns
            .interleave(columns.zipWithNext { left, right ->
                if ((left is AlignedColumn && left.alignment == LEFT) || (right is AlignedColumn && right.alignment == RIGHT))
                    SpacingColumn(left, right)
                else
                    null
            })
            .run {
                val first = first()
                if (first is AlignedColumn && first.alignment == RIGHT)
                    listOf(SpacingColumn(null, first)).plus(this) else this
            }
            .run {
                val last = last()
                if (last is AlignedColumn && last.alignment == LEFT)
                    plus(SpacingColumn(last, null)) else this
            }.filterNotNull()

        if (title != null) appendln("$title ```")
        else appendln("```")

        if (displayHeader) {
            // Draw table header
            appendRow(spacedColumns) {
                when (this) {
                    is DividerColumn -> "$border"
                    else -> header
                }
            }

            // Draw header border
            appendRow(spacedColumns, '═') {
                when (this) {
                    is EmojiColumn -> borderEmoji
                    is DividerColumn -> "$intersection"
                    else -> '═'.repeat(headerLength)
                }
            }
        }

        // Draw table body
        (0 until amountOfRows).forEach { row ->
            appendRow(spacedColumns) { cells[row] }
        }
        appendln("```")
    }.toString()
}

inline fun table(init: Table.() -> Unit): String = Table().also(init).toString()
inline fun StringBuilder.appendTable(init: Table.() -> Unit): StringBuilder = append(Table().also(init).toString())
inline fun StringBuilder.appendRow(
    columns: List<Column>,
    spacingChar: Char = ' ',
    transform: Column.() -> String
) {
    columns.forEach { column ->
        if (column is AlignedColumn) append(spacingChar.repeat(column.leftPadding))
        append(column.transform())
        if (column is AlignedColumn && column != columns.last()) append(spacingChar.repeat(column.rightPadding))
    }
    appendln()
}