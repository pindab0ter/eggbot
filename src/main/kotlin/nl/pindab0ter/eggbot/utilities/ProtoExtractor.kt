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
    val propertyPattern = Regex("""(?:\x0A[^\w]{0,2})([a-z_]*?.*?)(?=\x12.\x0A|$)""", DOT_MATCHES_ALL)

    val word = Regex("""[a-zA-Z_.]+""")
    val classProperty = Regex("""\.((\w+|\.)+)""")
    val propertyAnchor = Regex("""[\x01-\x03]\x28""")

    val messageLines = messagePattern.findAll(protobufSection).map { it.groupValues[1] }

    data class Property(
        val name: String,
        val index: Int,
        val type: String,
        val repeated: Boolean
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
            properties = propertyPattern.findAll(messageLine)
                .map { propertyLine -> propertyLine.groupValues[1] }
                .mapNotNull { propertyLine ->
                    val wordResult = word.findAll(propertyLine)
                    val classPropertyResult = classProperty.find(propertyLine)
                    val anchorPosition = propertyAnchor.find(propertyLine)?.range?.first ?: -1
                    when {
                        classPropertyResult != null && wordResult.first() != classPropertyResult -> Property(
                            name = wordResult.first().value,
                            index = propertyLine.getOrNull(propertyLine.indexOf('\u0018') + 1)?.toInt() ?: -1,
                            type = classProperty.find(propertyLine)!!.groups[1]!!.value,
                            repeated = propertyLine[anchorPosition] == '\u0003'
                        )
                        propertyLine.contains('\u0018') && propertyLine.contains('\u0028') -> Property(
                            name = word.find(propertyLine)!!.value,
                            index = propertyLine.getOrNull(propertyLine.indexOf('\u0018') + 1)?.toInt() ?: -1,
                            type = propertyLine[anchorPosition + 2].toInt().toType(),
                            repeated = propertyLine[anchorPosition] == '\u0003'
                        )
                        else -> null
                    }
                }
                .toList()
        )
    }

    println(messages.joinToString("\n\n") { message ->
        "message ${message.name} {\n${message.properties.sortedBy { it.index }.joinToString("\n") { property ->
            "    ${if (property.repeated) "repeated " else ""}${property.type} ${property.name} = ${property.index};"
        }}\n}"
    })
}