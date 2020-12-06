package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import nl.pindab0ter.eggbot.controller.*
import nl.pindab0ter.eggbot.model.Config

// TODO: Log error to channel and ping the admin user
val commandClient: CommandClient = CommandClientBuilder().apply {
    setOwnerId(Config.botOwnerId)
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
        CoopRemove,
        EarningsBonus,
        Inactive,
        Inactives,
        LeaderBoard,
        Migrate,
        Register,
        RollCall,
        RollClear,
        SoloInfo,
        SolosInfo,
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
