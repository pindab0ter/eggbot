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
    if (Config.activity != null) setActivity(Config.activity)
    addCommands(
        Active,
        ContractIDs,
        CoopAdd,
        CoopInfo,
        CoopsInfo,
        CoopRename,
        CoopRemove,
        EarningsBonus,
        Inactive,
        Inactives,
        LeaderBoard,
        Register,
        RollCall,
        RollClear,
        SoloInfo,
        Unregister,
        WhoIs
    )
    if (Config.devMode) addCommand(Test)
    setEmojis(
        Config.emojiSuccess,
        Config.emojiWarning,
        Config.emojiError
    )
}.build()
