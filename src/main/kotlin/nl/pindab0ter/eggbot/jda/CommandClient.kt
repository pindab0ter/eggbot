package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.commands.*

val commandClient: CommandClient = CommandClientBuilder().apply {
    setOwnerId(Config.ownerId)
    setPrefix(Config.prefix)
    setHelpWord(Config.helpWord)
    useHelpBuilder(true)
    setHelpConsumer(HelpConsumer)
    if (Config.game != null) setGame(Config.game)
    addCommands(
        Active,
        ContractIDs,
        CoopInfo,
        EarningsBonus,
        Inactive,
        LeaderBoard,
        Register,
        RollCall,
        SoloInfo,
        WhoIs
    )
    setEmojis(
        Config.emojiSuccess,
        Config.emojiWarning,
        Config.emojiError
    )
}.build()