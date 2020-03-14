package nl.pindab0ter.eggbot.utilities

import java.io.File

const val START = "/Users/auxbrain/dev/egginc/game/common/generated/protocol_buffers/ei.pb.cc"
const val END = "\u0064\u0000\u0000\u0018"

fun main(args: Array<String>) {
    val protoBufDefinition = File(args.first()).let { file ->
        require(file.name == "libegginc.so") { "Provide path to libegginc.so" }
        val text = file.bufferedReader().readText()
        val startPosition = text.indexOf(START, ignoreCase = false)
        val endPosition = text.indexOf(END)
        require(startPosition != -1) { "Could not find start position" }
        require(endPosition != -1) { "Could not find end position" }
        text.substring(startPosition.plus(START.length), endPosition)
    }

    val typeSeparator = Regex("""[\x02\x22]((?!\x12).){1,2}\x0A.""")
    val propertySeparator = Regex("""\x12.\x0A.""")
    val enumSeparator = Regex("""\x2A.{1,2}\x0A.""")

    val word = Regex("""[a-zA-Z_.]+""")
    val classProperty = Regex("""\.((\w+|\.)+)""")

    val separatedByType = protoBufDefinition.split(typeSeparator).drop(1)

    data class Property(
        val name: String,
        val index: Int,
        val type: String
    )

    data class Type(
        val name: String,
        val properties: List<Property>
    )

    fun Int.toType(): String = when (this) {
        1 -> "double"
        8 -> "bool"
        9 -> "string"
        else -> "??? ($this)"
    }

    val classes = separatedByType.map { line ->
        Type(
            name = word.find(line)!!.value,
            properties = line.split(propertySeparator).mapNotNull { line ->
                val wordResult = word.findAll(line)
                val classPropertyResult = classProperty.find(line)
                when {
                    classPropertyResult != null && wordResult.first() != classPropertyResult -> Property(
                        name = wordResult.first().value,
                        index = line.getOrNull(line.indexOf('\u0018') + 1)?.toInt() ?: -1,
                        type = classProperty.find(line)!!.groups[1]!!.value
                    )
                    line.contains('\u0018') && line.contains('\u0028') -> Property(
                        name = word.find(line)!!.value,
                        index = line.getOrNull(line.indexOf('\u0018') + 1)?.toInt() ?: -1,
                        type = line[line.indexOf('\u0028') + 1].toInt().toType()
                    )
                    else -> null
                }
            }
        )
    }

    println(classes.joinToString("\n\n") { type ->
        "message ${type.name} {\n${type.properties.joinToString("\n") { property ->
            "    ${property.type} ${property.name} = ${property.index};"
        }}\n}"
    })
}