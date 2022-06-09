package nl.pindab0ter.eggbot.converters


import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.ConverterToOptional
import com.kotlindiscord.kord.extensions.commands.converters.OptionalConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.commands.converters.builders.ConverterBuilder
import com.kotlindiscord.kord.extensions.commands.converters.builders.OptionalConverterBuilder
import com.kotlindiscord.kord.extensions.parser.StringParser
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Choice converter for database Farmers.
 */
class FarmerConverter(
    override var validator: Validator<Farmer> = null,
    val database: Database?
) : SingleConverter<Farmer>() {
    override val signatureTypeString: String = "Egg, Inc. Farmer"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        return false
    }

    override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
        parsed = transaction { Farmer.find { Farmers.id eq option.value as? String }.firstOrNull() }
            ?: throw Exception("Farmer not found")

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder = StringChoiceBuilder(
        name = arg.displayName,
        description = arg.description
    )
}

class FarmerConverterBuilder : ConverterBuilder<Farmer>() {
    var database: Database? = null

    override fun build(arguments: Arguments): SingleConverter<Farmer> {
        return arguments.arg(
            displayName = name,
            description = description,

            converter = FarmerConverter(
                validator = validator,
                database = database,
            ).withBuilder(this)
        )
    }
}

class OptionalFarmerConverterBuilder : OptionalConverterBuilder<Farmer>() {
    var database: Database? = null

    @OptIn(ConverterToOptional::class)
    override fun build(arguments: Arguments): OptionalConverter<Farmer> {
        return arguments.arg(
            displayName = name,
            description = description,

            converter = FarmerConverter(
                validator = validator,
                database = database,
            ).toOptional(
                outputError = !ignoreErrors,
                nestedValidator = validator,
            ).withBuilder(this)
        )
    }
}

fun Arguments.farmer(
    body: FarmerConverterBuilder.() -> Unit
): SingleConverter<Farmer> {
    val builder = FarmerConverterBuilder()

    body(builder)

    builder.validateArgument()

    return builder.build(this)
}

fun Arguments.optionalFarmer(
    body: OptionalFarmerConverterBuilder.() -> Unit
): OptionalConverter<Farmer> {
    val builder = OptionalFarmerConverterBuilder()

    body(builder)

    builder.validateArgument()

    return builder.build(this)
}
