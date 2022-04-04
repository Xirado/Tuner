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

package at.xirado.tuner.listener

import at.xirado.tuner.Application
import at.xirado.tuner.util.await
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class VoiceListener(val application: Application) : ListenerAdapter() {

    private val log = LoggerFactory.getLogger(VoiceListener::class.java) as Logger

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        val jda = event.jda
        val guild = event.guild
        val audioManager = application.audioManager

        val botVc = guild.audioManager.connectedChannel ?: return

        if (botVc != event.channelLeft)
            return

        val channel = event.channelLeft

        if (!audioManager.isGuildPlayerLoaded(guild.idLong))
            return

        val player = audioManager.getPlayer(guild)

        val userSize = channel.members.size

        if (userSize == 1 && player.player.playingTrack == null) {
            guild.audioManager.closeAudioConnection()
            player.destroy()
            return
        }

        player.player.isPaused = true

        application.coroutineScope.launch {

            val joinEvent = jda.await<GuildVoiceJoinEvent>(30.seconds) { it.channelJoined == channel }

            if (joinEvent == null) {
                guild.audioManager.closeAudioConnection()
                player.destroy()
                log.info("Nobody joined the channel in time!")
            } else {
                player.player.isPaused = false
                log.info("Someone joined! Unpausing!")
            }
        }
    }
}