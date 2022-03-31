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

package at.xirado.tuner.audio;

import at.xirado.tuner.Application;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class AudioScheduler extends AudioEventAdapter {

    private final Application application;
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private final GuildPlayer guildPlayer;
    private final long guildId;
    private boolean repeat = false;
    private boolean shuffle = false;
    private AudioTrack lastTrack;

    public AudioScheduler(Application application, AudioPlayer player, long guildId, GuildPlayer guildPlayer) {
        this.application = application;
        this.guildId = guildId;
        this.player = player;
        this.queue = new LinkedBlockingDeque<>();
        this.guildPlayer = guildPlayer;
    }

    public void queue(AudioTrack track) {
        if (player.getPlayingTrack() != null)
            queue.offer(track);
        else
            player.playTrack(track);
    }

    public void nextTrack() {
        if (repeat) {
            player.playTrack(lastTrack.makeClone());
            return;
        }
        AudioTrack track = queue.poll();
        if (track != null)
            player.playTrack(track);
        else
            player.stopTrack();
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public MessageEmbed getEmbed()
    {
        AudioTrack track = player.getPlayingTrack();

        String nowPlaying = track == null ? "Nothing playing" : track.getInfo().title + " - " + track.getInfo().author;
        return new EmbedBuilder()
                .setDescription(nowPlaying)
                .setColor(Color.BLUE)
                .build();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext)
            nextTrack();
    }

    public long getGuildId() {
        return guildId;
    }
}