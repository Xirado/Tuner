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
import dev.minn.jda.ktx.await
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.voice.*
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class VoiceListener(val application: Application) : ListenerAdapter() {

    private val log = LoggerFactory.getLogger(VoiceListener::class.java) as Logger

    /**
     * For when a member gets moved or leaves the channel
     */
    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (event.channelLeft == null)
            return

        if (event.member == event.guild.selfMember)
            return

        val state = event.guild.selfMember.voiceState!!

        if (state.channel == null)
            return

        if (state.channel != event.channelLeft)
            return

        if (event.channelLeft!!.members.size > 1)
            return

        if (!application.audioManagers[event.jda.selfUser.idLong]!!.isGuildPlayerLoaded(event.guild.idLong))
            return

        val audioPlayer = application.audioManagers[event.jda.selfUser.idLong]!!.getPlayer(event.guild)
        if (audioPlayer.player.playingTrack != null) {
            audioPlayer.player.isPaused = true
        } else {
            audioPlayer.destroy()
            event.guild.audioManager.closeAudioConnection()
            return
        }
        application.coroutineScope.launch {

            val updateEvent = event.jda.await<GenericGuildVoiceUpdateEvent>(30.seconds) {
                if (it.channelJoined == null)
                    return@await false
                if (it.channelJoined != event.channelLeft)
                    return@await false
                return@await it.member != it.guild.selfMember
            }

            if (updateEvent != null) {
                audioPlayer.player.isPaused = false
            } else {
                if (state.channel != null && state.channel!!.members.size > 1)
                    return@launch
                if (state.channel != null && state.channel == event.channelLeft) {
                    audioPlayer.destroy()
                    event.guild.audioManager.closeAudioConnection()
                }
            }
        }
    }

    /**
     * For when the bot gets moved to another channel
     */
    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.member != event.guild.selfMember)
            return

        if (!application.audioManagers[event.jda.selfUser.idLong]!!.isGuildPlayerLoaded(event.guild.idLong))
            return

        val audioPlayer = application.audioManagers[event.jda.selfUser.idLong]!!.getPlayer(event.guild)

        audioPlayer.player.isPaused = false

        if (event.channelJoined.members.size == 1) {
            val voiceState = event.guild.selfMember.voiceState!!

            if (audioPlayer.player.playingTrack != null) {
                audioPlayer.player.isPaused = true
            }
            application.coroutineScope.launch {
                val updateEvent = event.jda.await<GenericGuildVoiceUpdateEvent>(30.seconds) {
                    if (it.channelJoined == null)
                        return@await false
                    if (it.channelJoined != event.channelJoined)
                        return@await false

                    return@await it.member != it.guild.selfMember
                }
                if (updateEvent != null) {
                    audioPlayer.player.isPaused = false
                } else {
                    if (voiceState.channel != null && voiceState.channel!!.members.size > 1)
                        return@launch
                    if (voiceState.channel != null && voiceState.channel == event.channelJoined) {
                        audioPlayer.destroy()
                        event.guild.audioManager.closeAudioConnection()
                    }
                }
            }
        }
    }

    /**
     * For when the bot leaves the channel
     */
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (event.member != event.guild.selfMember)
            return

        if (!application.audioManagers[event.jda.selfUser.idLong]!!.isGuildPlayerLoaded(event.guild.idLong))
            return

        val audioPlayer = application.audioManagers[event.jda.selfUser.idLong]!!.getPlayer(event.guild)

        audioPlayer.destroy()
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.member != event.guild.selfMember)
            return

        if (!event.guild.selfMember.voiceState!!.isGuildDeafened) {
            application.coroutineScope.launch {
                try {
                    event.guild.deafen(event.guild.selfMember, true).await()
                } catch (_: Exception) {}
            }
        }
    }
}