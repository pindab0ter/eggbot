package nl.pindab0ter.eggbot.kord.converters


import com.auxbrain.ei.Contract
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceConverter
import com.kotlindiscord.kord.extensions.parser.StringParser
import dev.kord.common.annotation.KordPreview
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import nl.pindab0ter.eggbot.model.AuxBrain

/**
 * Choice converter for AuxBrain contracts.
 */
@OptIn(KordPreview::class)
class ContractChoiceConverter(
    choices: Array<Contract>,
    override var validator: Validator<Contract> = null,
) : ChoiceConverter<Contract>(choices.associateBy { contract -> contract.name }) {
    override val signatureTypeString: String = Contract::name.name

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        parsed = choices.values.firstOrNull { contract -> contract.name == arg }
            ?: throw Exception("Could not get contract information")

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder = StringChoiceBuilder(
        name = arg.displayName,
        description = arg.description).apply {
        required = true
        this@ContractChoiceConverter.choices.forEach { choice(it.key, it.value.name) }
    }
}

/**
 * Creates a contract choice converter, for a defined set of single arguments.
 *
 * @see ContractChoiceConverter
 */
fun Arguments.contractChoice(): SingleConverter<Contract> {
    val choices = AuxBrain.getContracts().toTypedArray()

    return arg(
        displayName = "contract",
        description = "Select an Egg, Inc. contract.",
        converter = ContractChoiceConverter(choices = choices)
    )
}