package nl.pindab0ter.eggbot.converters


import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.commands.converters.builders.ConverterBuilder
import com.kotlindiscord.kord.extensions.parser.StringParser
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import nl.pindab0ter.eggbot.model.AuxBrain

/**
 * Choice converter for AuxBrain contracts.
 */
class ContractChoiceConverter(
    override var validator: Validator<Contract> = null,
    val suppliedChoices: Map<String, Contract>,
) : ChoiceConverter<Contract>(choices = suppliedChoices) {
    override val signatureTypeString: String = Contract::name.name

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        parsed = choices.values.firstOrNull { contract -> contract.name == arg }
            ?: throw Exception("Could not get contract information")

        return true
    }

    override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
        parsed = choices.values.firstOrNull { contract -> contract.name.trim() == option.value }
            ?: throw Exception("Could not get contract information")

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder = StringChoiceBuilder(
        name = arg.displayName,
        description = arg.description
    ).apply {
        required = true
        this@ContractChoiceConverter.choices.forEach { choice(it.key, it.value.name) }
    }
}

class ContractConverterBuilder : ConverterBuilder<Contract>() {
    override fun build(arguments: Arguments): SingleConverter<Contract> {
        return arguments.arg(
            displayName = name,
            description = description,

            converter = ContractChoiceConverter(
                validator = validator,
                suppliedChoices = AuxBrain
                    .getContracts()
                    .sortedByDescending { contract -> contract.expirationTime }
                    .toTypedArray()
                    .associateBy { contract -> contract.name }
            ).withBuilder(this)
        )
    }
}

class CoopContractConverterBuilder : ConverterBuilder<Contract>() {
    override fun build(arguments: Arguments): SingleConverter<Contract> {
        return arguments.arg(
            displayName = name,
            description = description,

            converter = ContractChoiceConverter(
                validator = validator,
                suppliedChoices = AuxBrain
                    .getContracts()
                    .filter { contract -> contract.coopAllowed }
                    .sortedByDescending { contract -> contract.expirationTime }
                    .toTypedArray()
                    .associateBy { contract -> contract.name }
            ).withBuilder(this)
        )
    }
}

fun Arguments.contract(
    body: ContractConverterBuilder.() -> Unit,
): SingleConverter<Contract> {
    val builder = ContractConverterBuilder()

    body(builder)

    builder.validateArgument()

    return builder.build(this)
}

fun Arguments.coopContract(
    body: CoopContractConverterBuilder.() -> Unit,
): SingleConverter<Contract> {
    val builder = CoopContractConverterBuilder()

    body(builder)

    builder.validateArgument()

    return builder.build(this)
}
