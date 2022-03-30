@file:JvmName("Main")
package at.xirado.tuner

import at.xirado.tuner.config.ConfigLoader
import at.xirado.tuner.config.TunerConfiguration
import at.xirado.tuner.interaction.InteractionHandler
import at.xirado.tuner.listener.InteractionListener
import at.xirado.tuner.log.DiscordWebhookAppender
import at.xirado.tuner.util.Util
import ch.qos.logback.classic.Level
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import dev.minn.jda.ktx.getDefaultScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

lateinit var application: Application

private val log = LoggerFactory.getLogger(Application::class.java) as Logger

class Application {

    private val log = LoggerFactory.getLogger(Application::class.java) as Logger

    val tunerConfig: TunerConfiguration
    val coroutineScope = getDefaultScope()
    val shardManager: ShardManager
    val interactionHandler: InteractionHandler

    init {
        application = this
        tunerConfig = TunerConfiguration(ConfigLoader.loadFileAsYaml("config.yml", true))
        DiscordWebhookAppender.init(this)
        if (tunerConfig.discordToken == null || tunerConfig.discordToken.isEmpty())
            throw IllegalArgumentException("config.yml does not contain \"discord_token\" property!")

        val token = tunerConfig.discordToken

        shardManager = DefaultShardManagerBuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES)
            .setBulkDeleteSplittingEnabled(false)
            .setGatewayEncoding(GatewayEncoding.ETF)
            .setActivity(Activity.listening("music"))
            .disableCache(CacheFlag.EMOTE)
            .setAudioSendFactory(NativeAudioSendFactory())
            .addEventListeners(InteractionListener(this))
            .build()

        interactionHandler = InteractionHandler(this)
    }

}

fun main(args: Array<String>) {
    if ("--noclear" !in args)
        Util.clearScreen()

    if ("--debug" in args) {
        log.info("Started with \"--debug\" argument.")
        (LoggerFactory.getLogger("ROOT") as ch.qos.logback.classic.Logger).level = Level.DEBUG
    }

    Thread.currentThread().name = "Tuner Main-Thread"
    Application()
}