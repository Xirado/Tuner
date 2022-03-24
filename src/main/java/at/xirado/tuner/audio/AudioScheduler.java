package at.xirado.tuner.audio;

import at.xirado.tuner.Application;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

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

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext)
            nextTrack();
    }

    public long getGuildId() {
        return guildId;
    }


}
