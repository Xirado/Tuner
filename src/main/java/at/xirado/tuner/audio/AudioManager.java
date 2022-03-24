package at.xirado.tuner.audio;

import at.xirado.tuner.Application;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.entities.Guild;

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
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    public synchronized GuildPlayer getPlayer(Guild guild) {
        return audioPlayers.computeIfAbsent(guild.getIdLong(), id -> {
            var player = new GuildPlayer(application, id, playerManager);
            guild.getAudioManager().setSendingHandler(player.getSendHandler());
            return player;
        });
    }

    public Set<GuildPlayer> getPlayers() {
        return Set.copyOf(audioPlayers.values());
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }
}
