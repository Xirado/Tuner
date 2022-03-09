package at.xirado.tuner.audio;

import at.xirado.tuner.Application;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.LavalinkPlayer;

public class GuildPlayer {

    private final Application application;
    private final LavalinkPlayer player;
    private final AudioScheduler scheduler;
    private final long guildId;
    private final JdaLink link;

    public GuildPlayer(Application application, long guildId) {
        this.application = application;
        this.guildId = guildId;
        this.link = application.getLavalink().getLink(String.valueOf(guildId));
        this.player = link.getPlayer();
        this.scheduler = new AudioScheduler(application, player, guildId, this);
        player.addListener(scheduler);
    }

    public AudioScheduler getScheduler() {
        return scheduler;
    }

    public LavalinkPlayer getPlayer() {
        return player;
    }

    public long getGuildId() {
        return guildId;
    }

    public JdaLink getLink() {
        return link;
    }

    public Application getApplication() {
        return application;
    }
}
