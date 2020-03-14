package nl.pindab0ter.eggbot.utilities

import java.io.File
import kotlin.text.RegexOption.*

const val START = "/Users/auxbrain/dev/egginc/game/common/generated/protocol_buffers/ei.pb.cc"
const val END = "\u000A\u0003Egg"

fun main(args: Array<String>) {
    val protobufSection = File(args.first()).let { file ->
        require(file.name == "libegginc.so") { "Provide path to libegginc.so" }
        val text = file.bufferedReader().readText()
        val startPosition = text.indexOf(START, ignoreCase = false)
        val endPosition = text.indexOf(END)
        require(startPosition != -1) { "Could not find start position" }
        require(endPosition != -1) { "Could not find end position" }
        text.substring(startPosition.plus(START.length), endPosition)
    }

    val messagePattern = Regex("""(?:\x0A.{0,2})([A-Z].*?)(?=\x0A.{0,2}[A-Z])""", DOT_MATCHES_ALL)
    val propertySeparator = Regex("""\x12.\x0A.""")

    val word = Regex("""[a-zA-Z_.]+""")
    val classProperty = Regex("""\.((\w+|\.)+)""")

    val messageLines = messagePattern.findAll(protobufSection).map { it.groupValues[1] }

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
        2 -> "float"
        5 -> "int32"
        4 -> "uint64"
        8 -> "bool"
        9 -> "string"
        13 -> "uint32"
        else -> "??? ($this)"
    }

    val messages = messageLines.map { messageLine ->
        Type(
            name = word.find(messageLine)!!.value,
            properties = messageLine.split(propertySeparator).mapNotNull { propertyLine ->
                val wordResult = word.findAll(propertyLine)
                val classPropertyResult = classProperty.find(propertyLine)
                when {
                    classPropertyResult != null && wordResult.first() != classPropertyResult -> Property(
                        name = wordResult.first().value,
                        index = propertyLine.getOrNull(propertyLine.indexOf('\u0018') + 1)?.toInt() ?: -1,
                        type = classProperty.find(propertyLine)!!.groups[1]!!.value
                    )
                    propertyLine.contains('\u0018') && propertyLine.contains('\u0028') -> Property(
                        name = word.find(propertyLine)!!.value,
                        index = propertyLine.getOrNull(propertyLine.indexOf('\u0018') + 1)?.toInt() ?: -1,
                        type = propertyLine[propertyLine.indexOf('\u0028') + 1].toInt().toType()
                    )
                    else -> null
                }
            }
        )
    }

    println(messages.joinToString("\n\n") { message ->
        "message ${message.name} {\n${message.properties.sortedBy { it.index }.joinToString("\n") { property ->
            "    ${property.type} ${property.name} = ${property.index};"
        }}\n}"
    })
}