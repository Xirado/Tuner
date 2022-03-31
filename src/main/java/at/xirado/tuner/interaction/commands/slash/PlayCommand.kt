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

package at.xirado.tuner.interaction.commands.slash

import at.xirado.tuner.interaction.SlashCommand
import at.xirado.tuner.util.getYoutubeMusicSearchResults
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.getOption
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.commands.Command
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import net.dv8tion.jda.api.interactions.commands.OptionType.*

class PlayCommand : SlashCommand("play", "plays something") {

    init {
        option(type = STRING, name = "query", description = "what to play", required = true, autoComplete = true)
        option(type = STRING, name = "provider", description = "where to search", choices = arrayOf(
            Command.Choice("Youtube (Default)", "ytsearch:"),
            Command.Choice("Spotify", "spsearch:"),
            Command.Choice("Soundcloud", "scsearch:")
        ))

        addRequiredUserPermissions(Permission.ADMINISTRATOR)
        addRequiredBotPermissions(Permission.ADMINISTRATOR)
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val member = event.member!!
        val voiceState = member.voiceState!!
        val manager = event.guild!!.audioManager

        if (manager.connectedChannel == null) {
            try {
                manager.openAudioConnection(voiceState.channel)
            } catch (exception: PermissionException) {
                event.hook.sendMessage("I do not have permission to join this channel!").await()
                return
            }
        }

        val audioManager = application.audioManager
        val playerManager = audioManager.playerManager
        val player = audioManager.getPlayer(event.guild)
        var query = event.getOption<String>("query")!!
        val prefix = event.getOption<String>("provider")?: "ytsearch:"

        query = if (query.startsWith("http://") || query.startsWith("https://")) query else prefix+query

        try {
            when (val result = playerManager.loadItemAsync(player, query)) {
                is AudioTrack -> {
                    player.scheduler.queue(result)
                    event.hook.sendMessage("Now playing ${result.info.title}").await()
                }
                is AudioPlaylist -> {
                    if (result.isSearchResult) {
                        val single = result.tracks[0]
                        player.scheduler.queue(single)
                        event.hook.sendMessage("Now playing ${single.info.title}").await()
                        return
                    }
                    result.tracks.forEach { player.scheduler.queue(it) }

                    event.hook.sendMessage("Now playing playlist ${result.name}").await()
                }
                else -> event.hook.sendMessage("I haven't found anything!").await()
            }
        } catch (exception: FriendlyException) {
            event.hook.sendMessage(exception.message!!).await()
        }
    }

    override suspend fun onAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        when (event.focusedOption.name) {
            "query" -> {
                val value = event.focusedOption.value
                if (value.isBlank())
                    return kotlin.run { event.replyChoiceStrings().await() }

                val results = getYoutubeMusicSearchResults(application, event.focusedOption.value)

                event.replyChoiceStrings(results).await()
            }
        }
    }

    private suspend fun AudioPlayerManager.loadItemAsync(orderingKey: Any, identifier: String) : AudioItem? = suspendCoroutine { continuation ->
        loadItemOrdered(orderingKey, identifier, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) = continuation.resume(track)
            override fun playlistLoaded(playlist: AudioPlaylist) = continuation.resume(playlist)
            override fun noMatches() = continuation.resume(null)
            override fun loadFailed(exception: FriendlyException) = continuation.resumeWithException(exception)
        })
    }
}