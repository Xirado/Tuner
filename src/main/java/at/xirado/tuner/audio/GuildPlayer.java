package at.xirado.tuner.audio;

import at.xirado.tuner.Application;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class GuildPlayer {

    private final Application application;
    private final AudioPlayer player;
    private final AudioScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;
    private final long guildId;

    public GuildPlayer(Application application, long guildId, AudioPlayerManager audioPlayerManager) {
        this.application = application;
        this.guildId = guildId;
        this.player = audioPlayerManager.createPlayer();
        this.sendHandler = new AudioPlayerSendHandler(player);
        this.scheduler = new AudioScheduler(application, player, guildId, this);
        player.addListener(scheduler);
    }

    public AudioScheduler getScheduler() {
        return scheduler;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public long getGuildId() {
        return guildId;
    }

    public Application getApplication() {
        return application;
    }

    public AudioPlayerSendHandler getSendHandler() {
        return sendHandler;
    }
}
