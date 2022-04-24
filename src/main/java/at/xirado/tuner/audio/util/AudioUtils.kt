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

package at.xirado.tuner.audio.util

import at.xirado.tuner.audio.GuildPlayer
import com.github.topislavalinkplugins.topissourcemanagers.ISRCAudioTrack
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.Embed
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.MarkdownSanitizer

class AudioUtils {

    companion object {

        fun getAddedToQueueMessageEmbed(player: GuildPlayer, item: AudioItem): MessageEmbed {
            val isPlaying = player.player.playingTrack != null

            val embedDescription =
                if (isPlaying)
                    "**Added to queue:** ${titleMarkdown(item)}"
                else
                    "**Now playing** ${titleMarkdown(item)}"

            return Embed {
                description = embedDescription
                color = 0x32cd32
                thumbnail = getArtworkUrl(item)
            }
        }

        fun getArtworkUrl(item: AudioItem): String {
            if (item is AudioPlaylist)
                return getArtworkUrl(item.tracks[0])

            if (item is ISRCAudioTrack)
                return item.artworkURL

            if (item is YoutubeAudioTrack)
                return "https://img.youtube.com/vi/${item.identifier}/sddefault.jpg"

            return ""
        }

        fun titleMarkdown(item: AudioItem): String {
            return when (item) {
                is AudioTrack -> MarkdownSanitizer.sanitize("[${item.info.title}](${item.info.uri})")
                is AudioPlaylist -> {
                    val trackInfo = item.tracks[0].getUserData(TrackInfo::class.java)
                    return MarkdownSanitizer.sanitize("[${item.name}](${trackInfo.playlistUrl})")
                }
                else -> item.toString()
            }
        }
    }
}