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

package at.xirado.tuner.interaction.commands.slash.music

import at.xirado.tuner.audio.util.AudioUtils
import at.xirado.tuner.audio.util.TrackInfo
import at.xirado.tuner.data.TunerUser
import at.xirado.tuner.interaction.CommandFlag.VOICE_CHANNEL_ONLY
import at.xirado.tuner.interaction.SlashCommand
import at.xirado.tuner.interaction.autocomplete.IAutocompleteChoice
import at.xirado.tuner.util.Util
import at.xirado.tuner.util.getYoutubeMusicSearchResults
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.getOption
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.commands.Command
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PlayCommand : SlashCommand("play", "plays something") {

    init {
        option<String>(name = "query", description = "what to play", required = true, autocomplete = true)
        option<String>(name = "provider", description = "where to search") {
            addChoices(
                Command.Choice("Youtube (Default)", "ytsearch:"),
                Command.Choice("Spotify", "spsearch:"),
                Command.Choice("Soundcloud", "scsearch:")
            )
        }

        commandFlags.add(VOICE_CHANNEL_ONLY)
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val member = event.member!!
        val voiceState = member.voiceState!!

        if (voiceState.channel == null) {
            event.hook.sendMessage(":x: You are not in a voice-channel!").setEphemeral(true).queue()
            return
        }

        val channel = voiceState.channel!!

        val connectedBots = application.multiBotManager.getConnectedBots(event.guild!!.idLong, channel.idLong)

        val jda = if (connectedBots.isEmpty()) {
            val availableBots = application.multiBotManager.getAvailableBots(event.guild!!.idLong)
            if (availableBots.isEmpty()) {
                event.hook.sendMessage(":x: Sorry, there is currently no bot available!").setEphemeral(true).queue()
                return
            }
            availableBots[0]
        } else {
            connectedBots[0]
        }

        val guild = jda.getGuildById(event.guild!!.idLong)!!

        val manager = guild.audioManager

        val user = TunerUser(guild.idLong, event.user.idLong)

        if (manager.connectedChannel == null) {
            try {
                manager.openAudioConnection(voiceState.channel)
            } catch (exception: PermissionException) {
                event.hook.sendMessage("I do not have permission to join this channel!").setEphemeral(true).await()
                return
            }
        }

        val audioManager = application.audioManagers[jda.selfUser.idLong]!!
        val playerManager = audioManager.playerManager
        val player = audioManager.getPlayer(guild)
        var query = event.getOption<String>("query")!!
        val prefix = event.getOption<String>("provider")?: "ytsearch:"

        if (!Util.isUrl(query))
            query = prefix+query

        try {
            when (val result = playerManager.loadItemAsync(player, query)) {
                is AudioTrack -> {
                    result.userData = TrackInfo(event.user.idLong, null)
                    event.hook.sendMessageEmbeds(AudioUtils.getAddedToQueueMessageEmbed(player, result)).queue()
                    player.scheduler.queue(result)
                    user.addSearchEntry("${result.info.title} - ${result.info.author}", result.info.uri, false)
                }
                is AudioPlaylist -> {
                    if (result.isSearchResult) {
                        val single = result.tracks[0]
                        single.userData = TrackInfo(event.user.idLong, null)
                        event.hook.sendMessageEmbeds(AudioUtils.getAddedToQueueMessageEmbed(player, single)).setEphemeral(true).queue()
                        player.scheduler.queue(single)
                        user.addSearchEntry(event.getOption<String>("query")!!, event.getOption<String>("query")!!, false)
                        return
                    }
                    result.tracks.forEach { it.userData = TrackInfo(event.user.idLong, query) }
                    event.hook.sendMessageEmbeds(AudioUtils.getAddedToQueueMessageEmbed(player, result)).await()
                    user.addSearchEntry(result.name, query, true)
                    result.tracks.forEach { player.scheduler.queue(it) }
                }
                else -> event.hook.sendMessage("Sorry, I haven't found anything! :(").await()
            }
        } catch (exception: FriendlyException) {
            event.hook.sendMessage(exception.message!!).await()
        }
    }

    override suspend fun onAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        when (event.focusedOption.name) {
            "query" -> {
                val user = TunerUser(event.guild!!.idLong, event.user.idLong)
                val value = event.focusedOption.value
                val results = mutableListOf<IAutocompleteChoice>()
                if (value.isBlank()) {
                    results.addAll(user.getSearchHistory(25))
                    return kotlin.run { event.replyChoices(results.stream().map { it.toJDAChoice() }.toList()).await() }
                }
                results.addAll(user.getSearchHistory(event.focusedOption.value, 25))
                getYoutubeMusicSearchResults(application, event.focusedOption.value).stream().limit((25-results.size).toLong()).forEach(results::add)

                event.replyChoices(results.stream().map { it.toJDAChoice() }.toList()).await()
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