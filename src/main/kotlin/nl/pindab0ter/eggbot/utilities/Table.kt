package nl.pindab0ter.eggbot.utilities

import nl.pindab0ter.eggbot.utilities.Table.ValueColumn.Aligned.LEFT
import nl.pindab0ter.eggbot.utilities.Table.ValueColumn.Aligned.RIGHT

@DslMarker
annotation class TableMarker

@TableMarker
class Table {
    private val columns: MutableList<Column> = mutableListOf()
    private val valueColumns: List<ValueColumn> get() = columns.filterIsInstance<ValueColumn>()
    private val amountOfRows: Int get() = (columns.first { it is ValueColumn } as? ValueColumn)?.cells?.size ?: 0

    private fun <T : Column> initColumn(column: T, init: T.() -> Unit): T {
        column.init()
        columns += column
        return column
    }

    fun valueColumn(init: ValueColumn.() -> Unit): ValueColumn = initColumn(ValueColumn(), init)
    fun dividerColumn(init: DividerColumn.() -> Unit): DividerColumn = initColumn(DividerColumn(), init)

    override fun toString(): String = StringBuilder().apply {
        require(valueColumns.isNotEmpty()) { "Table must have ValueColumns" }
        require(valueColumns.all { it.cells?.size == amountOfRows }) { "All columns must be of equal size" }

        // Add SpacingColumns between adjacent left and right aligned columns
        val spacedColumns: List<Column> = columns.interleave(columns.zipWithNext { left, right ->
            if (left is ValueColumn && left.alignment == LEFT && right is ValueColumn && right.alignment == RIGHT)
                SpacingColumn(left, right)
            else
                null
        }).filterNotNull()

        // Draw table header
        appendRow(spacedColumns) {
            when (this) {
                is DividerColumn -> divider
                is SpacingColumn -> header
                is ValueColumn -> header
                else -> ""
            }
        }

        // Draw header border
        appendRow(spacedColumns, '═') {
            when (this) {
                is DividerColumn -> intersection
                is SpacingColumn -> '═'.repeat(header.length)
                is ValueColumn -> '═'.repeat(header.length)
                else -> ""
            }
        }

        // Draw table body
        (0 until amountOfRows).forEach { row ->
            appendRow(spacedColumns) {
                when (this) {
                    is DividerColumn -> divider
                    is SpacingColumn -> spacing[row]
                    is ValueColumn -> cells!![row]
                    else -> ""
                }
            }
        }
    }.toString()

    @TableMarker
    interface Column

    abstract class SuppliedColumn : Column {
        var leftPadding = 0
        var rightPadding = 1
    }

    class ValueColumn : SuppliedColumn() {
        enum class Aligned { LEFT, RIGHT }

        var header: String = ""
        var alignment: Aligned = LEFT
        var cells: List<String>? = null
    }

    open class DividerColumn : SuppliedColumn() {
        var divider: String = "│"
        var intersection: String = "╪"
    }

    private class SpacingColumn(left: ValueColumn, right: ValueColumn) : Column {
        val header: String
        val spacing: List<String>

        init {
            require(left.alignment == LEFT && right.alignment == RIGHT) { "Can only space opposing columns" }
            require(left.cells != null && right.cells != null) { "Values can not be null" }

            val (header, spacing) = left.widths!!.zip(right.widths!!).let { rows ->
                val widestPair = rows.map { (a, b) -> a + b }.max()!!

                ' '.repeat(widestPair - (left.header.length + right.header.length)) to rows.map { (a, b) ->
                    ' '.repeat(widestPair - (a + b))
                }
            }

            this.header = header
            this.spacing = spacing
        }

        // Moved here instead of in ValueColumn to prevent visibility from constructor lambda
        val ValueColumn.widths: List<Int>? get() = cells?.plus(header)?.map { row -> row.length }
    }
}

inline fun table(init: Table.() -> Unit): String = Table().also(init).toString()
