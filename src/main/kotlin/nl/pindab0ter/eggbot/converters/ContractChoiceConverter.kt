package nl.pindab0ter.eggbot.converters


import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.CommandContext
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
class ContractChoiceConverter(override var validator: Validator<Contract> = null) : SingleConverter<Contract>() {
    override val signatureTypeString: String = Contract::name.name

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val contractId: String = named ?: parser?.parseNext()?.data ?: return false

        val contracts = AuxBrain.getContracts()

        parsed = contracts.firstOrNull { contract -> contract.id.trim() == contractId }
            ?: throw DiscordRelayedException("Could not find contract “$contractId”.")

        return true
    }

    override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
        parsed = AuxBrain.getContracts().firstOrNull { contract -> contract.id.trim() == option.value }
            ?: throw DiscordRelayedException("Could not find contract “${option.value}”.")

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder = StringChoiceBuilder(
        name = arg.displayName,
        description = arg.description,
    ).apply {
        required = true
    }
}

class ContractConverterBuilder : ConverterBuilder<Contract>() {
    override fun build(arguments: Arguments): SingleConverter<Contract> {
        return arguments.arg(
            displayName = name,
            description = description,
            converter = ContractChoiceConverter(validator).withBuilder(this)
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
