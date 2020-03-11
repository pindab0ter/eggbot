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
    var title: String? = null
    var displayHeader: Boolean = true

    private fun <T : Column> initColumn(column: T, init: T.() -> Unit): T {
        column.init()
        columns += column
        return column
    }

    fun column(init: ValueColumn.() -> Unit): ValueColumn = initColumn(ValueColumn(), init)
    fun divider(init: DividerColumn.() -> Unit): DividerColumn = initColumn(DividerColumn(), init)

    override fun toString(): String = StringBuilder().apply {
        require(valueColumns.isNotEmpty()) { "Table must have ValueColumns" }
        require(valueColumns.all { it.cells?.size == amountOfRows }) { "All columns must be of equal size" }

        // Add SpacingColumns between adjacent left and right aligned columns
        val spacedColumns: List<Column> = columns.interleave(columns.zipWithNext { left, right ->
            if ((left is ValueColumn && left.alignment == LEFT) || (right is ValueColumn && right.alignment == RIGHT))
                SpacingColumn(left, right)
            else
                null
        }).filterNotNull()

        if (title != null) appendln("$title ```")
        else appendln("```")

        if (displayHeader) {
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
        appendln("```")
    }.toString()

    @TableMarker
    interface Column

    abstract class SuppliedColumn : Column {
        var leftPadding = 0
        var rightPadding = 0
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

    private class SpacingColumn(left: Column, right: Column) : Column {
        val header: String
        val spacing: List<String>

        init {
            val (header, spacing) = when {
                left is ValueColumn && left.alignment == LEFT && right is ValueColumn && right.alignment == RIGHT ->
                    left.widths.zip(right.widths).let { rows ->
                        val widestPair = rows.map { (a, b) -> a + b }.max()!!

                        ' '.repeat(widestPair - (left.header.length + right.header.length)) to rows.map { (a, b) ->
                            ' '.repeat(widestPair - (a + b))
                        }
                    }
                left is ValueColumn && left.alignment == LEFT ->
                    left.widest.let { widest ->
                        ' '.repeat(widest - left.header.length) to left.widths.map { width ->
                            ' '.repeat(widest - width)
                        }
                    }
                right is ValueColumn && right.alignment == RIGHT ->
                    right.widest.let { widest ->
                        ' '.repeat(widest - right.header.length) to right.widths.map { width ->
                            ' '.repeat(widest - width)
                        }
                    }
                else -> "" to listOf("")
            }

            this.header = header
            this.spacing = spacing
        }

        // Moved here instead of in ValueColumn to prevent visibility from constructor lambda
        val ValueColumn.widths: List<Int> get() = cells?.plus(header)?.map { row -> row.length } ?: emptyList()
        val ValueColumn.widest: Int get() = cells?.plus(header)?.map { row -> row.length }?.max() ?: 0
    }
}

inline fun table(init: Table.() -> Unit): String = Table().also(init).toString()
inline fun StringBuilder.appendTable(init: Table.() -> Unit): StringBuilder = append(Table().also(init).toString())
