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

import at.xirado.tuner.audio.AudioManager
import at.xirado.tuner.config.ConfigLoader
import at.xirado.tuner.config.TunerConfiguration
import at.xirado.tuner.interaction.InteractionHandler
import at.xirado.tuner.listener.InteractionListener
import at.xirado.tuner.listener.ReadyListener
import at.xirado.tuner.listener.VoiceListener
import at.xirado.tuner.log.DiscordWebhookAppender
import at.xirado.tuner.util.Util
import ch.qos.logback.classic.Level
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import dev.minn.jda.ktx.getDefaultScope
import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.cache.CacheFlag
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Application {

    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java) as Logger
        lateinit var application: Application
    }

    val tunerConfig: TunerConfiguration
    val coroutineScope = getDefaultScope()
    val shardManager: ShardManager
    val interactionHandler: InteractionHandler
    val audioManager: AudioManager
    val httpClient = OkHttpClient()

    init {
        application = this
        tunerConfig = TunerConfiguration(ConfigLoader.loadFileAsYaml("config.yml", true))
        DiscordWebhookAppender.init(this)
        if (tunerConfig.discordToken == null || tunerConfig.discordToken.isEmpty())
            throw IllegalArgumentException("config.yml does not contain \"discord_token\" property!")

        val token = tunerConfig.discordToken

        shardManager = DefaultShardManagerBuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES)
            .setBulkDeleteSplittingEnabled(false)
            .setGatewayEncoding(GatewayEncoding.ETF)
            .setActivity(Activity.listening("music"))
            .disableCache(CacheFlag.EMOTE)
            .setAudioSendFactory(NativeAudioSendFactory())
            .addEventListeners(InteractionListener(this), ReadyListener(this), VoiceListener(this))
            .build()

        interactionHandler = InteractionHandler(this)
        audioManager = AudioManager(this)
    }
}

fun main(args: Array<String>) {
    if ("--noclear" !in args)
        Util.clearScreen()

    if ("--debug" in args)
        (LoggerFactory.getLogger("ROOT") as ch.qos.logback.classic.Logger).level = Level.DEBUG

    Thread.currentThread().name = "Tuner Main-Thread"
    Application()
}