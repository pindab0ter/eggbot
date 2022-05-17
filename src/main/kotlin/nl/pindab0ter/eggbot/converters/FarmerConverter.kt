package nl.pindab0ter.eggbot.converters


import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceConverter
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
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Choice converter for database Farmers.
 */
class FarmerConverter(
    override var validator: Validator<Farmer> = null,
) : ChoiceConverter<Farmer>(choices = transaction {
    Farmer.find { Farmers.inGameName.isNotNull() }
        .limit(25)
        .orderBy(Farmers.updatedAt to DESC)
        .associateBy { farmer -> farmer.inGameName!! }
}) {
    override val signatureTypeString: String = "Egg, Inc. Farmer"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        parsed = choices.values.firstOrNull { farmer -> farmer.inGameName == arg }
            ?: throw Exception("Could not get farmer information")

        return true
    }

    override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
        parsed = choices.values.firstOrNull { farmer -> farmer.inGameName?.trim() == option.value }
            ?: throw Exception("Could not get farmer information")

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder = StringChoiceBuilder(
        name = arg.displayName,
        description = arg.description
    ).apply {
        required = true
        this@FarmerConverter.choices.forEach { choice(it.key, it.value.inGameName!!) }
    }
}

class FarmerConverterBuilder : ConverterBuilder<Farmer>() {
    override fun build(arguments: Arguments): SingleConverter<Farmer> {
        return arguments.arg(
            displayName = name,
            description = description,

            converter = FarmerConverter(
                validator = validator,
            ).withBuilder(this)
        )
    }
}

class OptionalFarmerConverterBuilder : OptionalConverterBuilder<Farmer>() {
    @OptIn(ConverterToOptional::class)
    override fun build(arguments: Arguments): OptionalConverter<Farmer> {
        return arguments.arg(
            displayName = name,
            description = description,

            converter = FarmerConverter(
                validator = validator,
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
