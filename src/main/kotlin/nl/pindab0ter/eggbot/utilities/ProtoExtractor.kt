package nl.pindab0ter.eggbot.utilities

import java.io.File
import kotlin.text.RegexOption.DOT_MATCHES_ALL

const val START = "ei.proto\u0012\u0002ei\u001A\u000Ccommon.proto"
const val END = "\u0000\u0000\u0000\u0000"

val componentPattern = """(?:\x0A.{0,2}?)(?<name>[A-Za-z]+)(?<body>.*?)(?=(?:\x0A.[A-Z][a-z]|$))"""
    .toRegex(DOT_MATCHES_ALL)
val classPattern = """[a-z0-9_]{3,}"""
    .toRegex()
val propertyPattern = """(?:\x0A.)(?<name>[A-Za-z0-9_]+).(?<index>.).*?(?<repeated>.)\x28(?<primitive>.)(?:\x32.\..+?\.(?<reference>[A-Za-z.]*))?"""
    .toRegex(DOT_MATCHES_ALL)
val constantPattern = """(?<name>[A-Z_]{2,})\x10(?<index>.).*?(?:\x0A|$)"""
    .toRegex(DOT_MATCHES_ALL)

data class Class(
    val name: String,
    val properties: List<Property>,
)

data class Property(
    val name: String,
    val index: Int,
    val type: String,
    val repeated: Boolean,
)

data class Enum(
    val name: String,
    val constants: List<Constant>,
)

data class Constant(
    val name: String,
    val index: Int,
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

fun main(args: Array<String>) {
    val protobufSection = File(args.first()).let { file ->
        require(file.name == "libegginc.so") { "Provide path to libegginc.so" }
        val text = file.bufferedReader().readText()
        val startPosition = text.indexOf(START, ignoreCase = false)
        require(startPosition != -1) { "Could not find start position" }
        val endPosition = text.indexOf(END, startPosition)
        require(endPosition != -1) { "Could not find end position" }
        text.substring(startPosition.plus(START.length), endPosition)
    }

    val components = componentPattern.findAll(protobufSection).map(MatchResult::groups).map { componentGroups ->
        if (classPattern.containsMatchIn(componentGroups["body"]!!.value)) Class(
            name = componentGroups["name"]!!.value,
            properties = propertyPattern.findAll(componentGroups["body"]!!.value)
                .map(MatchResult::groups)
                .map { classGroups ->
                    Property(
                        name = classGroups["name"]!!.value,
                        index = classGroups["index"]!!.value.first().toInt(),
                        type = classGroups["reference"]?.value ?: classGroups["primitive"]!!.value.first().toInt()
                            .toType(),
                        repeated = classGroups["repeated"]!!.value.first() == '\u0003'
                    )
                }.toList()
        )
        else Enum(
            name = componentGroups["name"]!!.value,
            constants = constantPattern.findAll(componentGroups["body"]!!.value)
                .map(MatchResult::groups)
                .map { enumGroups ->
                    Constant(
                        name = enumGroups["name"]!!.value,
                        index = enumGroups["index"]?.value?.first()?.toInt() ?: -1
                    )
                }.toList()
        )
    }

    //@formatter:off
    println(components.joinToString("\n\n") { component ->
        when (component) {
            is Enum -> """
                |enum ${component.name} {
                |${component.constants.sortedBy { it.index }.joinToString("\n") { constant ->
                    "  ${constant.name} = ${constant.index};"
                }}
                |}""".trimMargin()
            is Class -> """
                |message ${component.name} {
                |${component.properties.sortedBy { it.index }.joinToString("\n") { property ->
                    "  ${if (property.repeated) "repeated " else ""}${property.type} ${property.name} = ${property.index};"
                }}
                |}""".trimMargin()
            else -> ""
        }
    })
    //@formatter:on
}
