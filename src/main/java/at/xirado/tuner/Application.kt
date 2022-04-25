/*
 * Copyright 2022 Marcel Korzonek and the Tuner contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("Main")
package at.xirado.tuner

import at.xirado.tuner.Application.Companion.application
import at.xirado.tuner.audio.AudioManager
import at.xirado.tuner.config.ConfigLoader
import at.xirado.tuner.config.TunerConfiguration
import at.xirado.tuner.data.GuildManager
import at.xirado.tuner.interaction.InteractionHandler
import at.xirado.tuner.interaction.MultiBotManager
import at.xirado.tuner.listener.InteractionListener
import at.xirado.tuner.listener.RegistrationListener
import at.xirado.tuner.listener.VoiceListener
import at.xirado.tuner.log.DiscordWebhookAppender
import at.xirado.tuner.util.Util
import ch.qos.logback.classic.Level
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import dev.minn.jda.ktx.getDefaultScope
import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class Application {

    companion object {
        val log = LoggerFactory.getLogger(Application::class.java) as Logger
        lateinit var application: Application
    }

    val tunerConfig: TunerConfiguration
    val coroutineScope = getDefaultScope()
    val bots = mutableMapOf<Long, JDA>()
    val audioManagers = mutableMapOf<Long, AudioManager>()
    val httpClient = OkHttpClient()
    val guildManager: GuildManager
    val multiBotManager: MultiBotManager
    val interactionHandler: InteractionHandler

    init {
        application = this
        tunerConfig = TunerConfiguration(ConfigLoader.loadFileAsYaml("config.yml", true))
        multiBotManager = MultiBotManager(this)
        interactionHandler = InteractionHandler(this)
        DiscordWebhookAppender.init(this)
        if (tunerConfig.discordTokens.isEmpty())
            throw IllegalArgumentException("\"discord_tokens\" property does not exist or is empty!")

        val tokens = tunerConfig.discordTokens

        tokens.forEachIndexed { index, token ->
            val contextMap = ConcurrentHashMap<String, String>()
            contextMap["jda.shard"] = "Shard $index"

            val userId = getUserIdFromToken(token)
            audioManagers[userId] = AudioManager(this, userId)
            bots[userId] = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES)
                .setBulkDeleteSplittingEnabled(false)
                .setGatewayEncoding(GatewayEncoding.ETF)
                .setActivity(Activity.listening("music (Shard ${index+1})"))
                .disableCache(CacheFlag.EMOTE)
                .setContextMap(contextMap)
                .setContextEnabled(true)
                .setAudioSendFactory(NativeAudioSendFactory())
                .addEventListeners(InteractionListener(this), RegistrationListener(this), VoiceListener(this))
                .build()
        }
        Class.forName("at.xirado.tuner.data.Database")
        guildManager = GuildManager(this)
    }
}

fun main(args: Array<String>) {
    if ("--noclear" !in args)
        Util.clearScreen()

    if ("--debug" in args)
        (LoggerFactory.getLogger("ROOT") as ch.qos.logback.classic.Logger).level = Level.DEBUG

    Thread.setDefaultUncaughtExceptionHandler { _, e -> Application.log.error("An unhandled exception was encountered", e)}
    Thread.currentThread().name = "Tuner Main-Thread"
    Application()
}

private fun getUserIdFromToken(token: String): Long {
    val userIdEncoded = token.split(".")[0]
    return String(Base64.getDecoder().decode(userIdEncoded), StandardCharsets.UTF_8).toLong()
}