package at.xirado.tuner.audio;

import at.xirado.tuner.Application;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AudioManager {

    private final Application application;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildPlayer> audioPlayers;

    public AudioManager(Application application) {
        this.application = application;
        this.playerManager = new DefaultAudioPlayerManager();
        this.audioPlayers = new ConcurrentHashMap<>();
    }

    public synchronized GuildPlayer getPlayer(long guildId) {
        return audioPlayers.computeIfAbsent(guildId, id -> new GuildPlayer(application, id));
    }

    public Set<GuildPlayer> getPlayers() {
        return Set.copyOf(audioPlayers.values());
    }

}
