package nl.pindab0ter.eggbot

import com.auxbrain.ei.Egg
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.GatewayIntent.*
import net.dv8tion.jda.api.utils.cache.CacheFlag.*
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.jda.CommandLogger
import nl.pindab0ter.eggbot.jda.commandClient
import nl.pindab0ter.eggbot.utilities.JobLogger
import nl.pindab0ter.eggbot.jobs.UpdateDiscordTagsJob
import nl.pindab0ter.eggbot.jobs.UpdateFarmersJob
import nl.pindab0ter.eggbot.jobs.UpdateLeaderBoardsJob
import nl.pindab0ter.eggbot.model.Config
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.CronScheduleBuilder.weeklyOnDayAndHourAndMinute
import org.quartz.DateBuilder.FRIDAY
import org.quartz.JobBuilder.newJob
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.sql.Connection
import java.time.ZoneId
import java.util.*


object EggBot {
    val log = KotlinLogging.logger {}

    // region Public values

    val jdaClient: JDA = JDABuilder
        .create(
            Config.botToken,
            DIRECT_MESSAGES,
            DIRECT_MESSAGE_REACTIONS,
            DIRECT_MESSAGE_TYPING,
            GUILD_MEMBERS,
            GUILD_MESSAGES,
            GUILD_MESSAGE_REACTIONS,
            GUILD_MESSAGE_TYPING,
        )
        .disableCache(ACTIVITY, CLIENT_STATUS, EMOTE, MEMBER_OVERRIDES, VOICE_STATE)
        .addEventListeners(CommandLogger)
        .build()
    val guild: Guild by lazy {
        jdaClient.getGuildById(Config.guildId) ?: throw Exception("Could not find guild with ID ${Config.guildId}")
    }
    val botOwner: Member? by lazy {
        guild.getMemberById(Config.botOwnerId)
    }
    val adminRole: Role by lazy {
        guild.getRoleById(Config.adminRoleId) ?: throw Exception("Could not find role with ID ${Config.adminRoleId}")
    }
    var clientVersion = Config.clientVersion
        set(value) {
            field = value
            log.warn { "Client version upgraded to $value" }
        }
    val botCommandsChannel: TextChannel by lazy {
        guild.getTextChannelById(Config.botCommandsChannelId)
            ?: throw Exception("Could not find channel with ID ${Config.botCommandsChannelId}")
    }
    val earningsBonusLeaderBoardChannel: TextChannel by lazy {
        guild.getTextChannelById(Config.earningsBonusLeaderBoardChannelId)
            ?: throw Exception("Could not find channel with ID ${Config.earningsBonusLeaderBoardChannelId}")
    }
    val soulEggsLeaderBoardChannel: TextChannel by lazy {
        guild.getTextChannelById(Config.soulEggsLeaderBoardChannelId)
            ?: throw Exception("Could not find channel with ID ${Config.soulEggsLeaderBoardChannelId}")
    }
    val prestigesLeaderBoardChannel: TextChannel by lazy {
        guild.getTextChannelById(Config.prestigesLeaderBoardChannelId)
            ?: throw Exception("Could not find channel with ID ${Config.prestigesLeaderBoardChannelId}")
    }
    val dronesLeaderBoardChannel: TextChannel by lazy {
        guild.getTextChannelById(Config.dronesLeaderBoardChannelId)
            ?: throw Exception("Could not find channel with ID ${Config.dronesLeaderBoardChannelId}")
    }
    val eliteDronesLeaderBoardChannel: TextChannel by lazy {
        guild.getTextChannelById(Config.eliteDronesLeaderBoardChannelId)
            ?: throw Exception("Could not find channel with ID ${Config.eliteDronesLeaderBoardChannelId}")
    }
    val eggsToEmotes: Map<Egg, Emote?> by lazy {
        Config.eggsToEmoteIds.mapValues { (_, emoteId) ->
            emoteId?.let { guild.getEmoteById(it) }
        }
    }
    val emoteGoldenEgg: Emote? by lazy { Config.emoteGoldenEggId?.let { id -> guild.getEmoteById(id) } }
    val emoteSoulEgg: Emote? by lazy { Config.emoteSoulEggId?.let { id -> guild.getEmoteById(id) } }
    val emoteProphecyEgg: Emote? by lazy { Config.emoteProphecyEggId?.let { id -> guild.getEmoteById(id) } }

    fun Egg.toEmote() = eggsToEmotes[this]?.asMention ?: "🥚"

    // endregion

    @JvmStatic
    fun main(args: Array<String>) {
        connectToDatabase()
        initializeDatabase()
        startScheduler()
        jdaClient.awaitReady()
        jdaClient.addEventListener(commandClient)
        log.info { "${jdaClient.selfUser.name} is ready for business!" }
    }

    private fun connectToDatabase() {
        Database.connect(
            url = "jdbc:sqlite:./EggBot.sqlite",
            driver = "org.sqlite.JDBC",
            setupConnection = { connection ->
                connection.createStatement().execute("PRAGMA foreign_keys = ON")
            })
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        KotlinLogging.logger("Exposed").info { "Connected to database" }
    }

    private fun initializeDatabase() = transaction {
        SchemaUtils.create(DiscordUsers)
        SchemaUtils.create(Farmers)
        SchemaUtils.create(Coops)
        SchemaUtils.create(CoopFarmers)
    }

    private fun startScheduler() = StdSchedulerFactory.getDefaultScheduler().apply {
        // Use Europe/London because it moves with Daylight Saving Time
        val london = TimeZone.getTimeZone(ZoneId.of("Europe/London"))

        log.info { "Starting scheduler…" }

        if (!Config.devMode) scheduleJob(
            newJob(UpdateFarmersJob::class.java)
                .withIdentity("update_farmers")
                .build(),
            newTrigger()
                .withIdentity("quarter_daily_farmer_update")
                .withSchedule(simpleSchedule().withIntervalInHours(6).repeatForever())
                .build()
        )
        if (!Config.devMode) scheduleJob(
            newJob(UpdateDiscordTagsJob::class.java)
                .withIdentity("update_discord_tags")
                .build(),
            newTrigger()
                .withIdentity("quarter_daily_discord_tags_update")
                .withSchedule(simpleSchedule().withIntervalInHours(6).repeatForever())
                .build()
        )
        scheduleJob(
            newJob(UpdateLeaderBoardsJob::class.java)
                .withIdentity("update_leader_board")
                .build(),
            newTrigger()
                .withIdentity("every_friday_at_noon")
                .withSchedule(
                    weeklyOnDayAndHourAndMinute(FRIDAY, 12, 0).inTimeZone(london)
                )
                .build()
        )
        listenerManager.addJobListener(JobLogger)
        start()
    }
}
