package nl.pindab0ter.eggbot.utilities

import nl.pindab0ter.eggbot.utilities.Table.AlignedColumn.Alignment.LEFT
import nl.pindab0ter.eggbot.utilities.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.utilities.Table.Column

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
    var topPadding: Int = 0
    var bottomPadding: Int = 0

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
                    left.cellLengths.zip(right.cellLengths).let { rowLengths ->
                        val widestPair = rowLengths
                            .plus(left.header.length to right.header.length)
                            .map { (a, b) -> a + b }.max()!!

                        ' '.repeat(widestPair - (left.headerLength + right.headerLength)) to rowLengths.map { (a, b) ->
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

    fun render(): List<String> {
        // TODO: Refactor
        val blocks = mutableListOf<String>()
        blocks.add(StringBuilder().apply {
            require(alignedColumns.filterIsInstance<ValueColumn>().isNotEmpty()) { "Table must have ValueColumns" }
            require(columns.all { it.cells.size == amountOfRows }) { "All columns must be of equal size" }

            repeat(topPadding) { appendln() }

            val spacedColumns: List<Column> = calculateSpacing(columns)

            if (title != null) appendln("$title ```")
            else appendln("```")

            if (displayHeader) {
                // Draw table header
                appendln(spacedColumns.renderRow {
                    when (this) {
                        is DividerColumn -> "$border"
                        else -> header
                    }
                })

                // Draw header border
                appendln(spacedColumns.renderRow('═') {
                    when (this) {
                        is EmojiColumn -> borderEmoji
                        is DividerColumn -> "$intersection"
                        else -> '═'.repeat(headerLength)
                    }
                })
            }

            // Draw table body
            (0 until amountOfRows).forEach { rowIndex ->
                val renderedRow = spacedColumns.renderRow { cells[rowIndex] }
                if (length + renderedRow.length + 3 > 2000) {
                    append("```\u200B")
                    blocks.add(toString())
                    clear()
                    appendln("```")
                }
                appendln(renderedRow)
            }
            append("```")

            repeat(bottomPadding) { appendln() }

        }.toString())
        return blocks
    }

    /** Add SpacingColumns between adjacent left and right aligned columns and before and after the last column **/
    private fun calculateSpacing(columns: List<Column>): List<Column> = columns
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
        }
        .filterNotNull()
}

inline fun table(init: Table.() -> Unit): List<String> =
    Table().also(init).render()

inline fun StringBuilder.appendTable(
    init: Table.() -> Unit
): StringBuilder = also {
    Table().also(init).render().let { blocks ->
        blocks.forEach { block ->
            append(block)
            if (block != blocks.last()) appendln()
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
