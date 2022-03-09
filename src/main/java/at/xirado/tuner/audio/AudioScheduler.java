package at.xirado.tuner.audio;

import at.xirado.tuner.Application;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavalinkPlayer;
import lavalink.client.player.event.PlayerEventListenerAdapter;
import net.dv8tion.jda.api.entities.AudioChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class AudioScheduler extends PlayerEventListenerAdapter {

    private final Application application;
    private final LavalinkPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private final GuildPlayer guildPlayer;
    private final long guildId;
    private boolean repeat = false;
    private boolean shuffle = false;
    private AudioTrack lastTrack;

    public AudioScheduler(Application application, LavalinkPlayer player, long guildId, GuildPlayer guildPlayer) {
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

    public LavalinkPlayer getPlayer() {
        return player;
    }

    @Override
    public void onTrackStart(IPlayer player, AudioTrack track) {
        lastTrack = track;
        AudioChannel current = application.getShardManager().getGuildById(guildId).getSelfMember().getVoiceState().getChannel();
    }

    @Override
    public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext)
            nextTrack();
    }

    @Override
    public void onTrackException(IPlayer player, AudioTrack track, Exception exception) {
        if (repeat)
            repeat = false;

        nextTrack();
    }

    public long getGuildId() {
        return guildId;
    }


}
