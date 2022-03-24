package at.xirado.tuner;

import at.xirado.tuner.audio.AudioManager;
import at.xirado.tuner.config.ConfigLoader;
import at.xirado.tuner.config.TunerConfiguration;
import at.xirado.tuner.db.Database;
import at.xirado.tuner.interaction.InteractionManager;
import at.xirado.tuner.listener.ReadyListener;
import at.xirado.tuner.listener.SlashCommandListener;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.sql.SQLException;

public class Application {

    private static Logger LOG = LoggerFactory.getLogger(Application.class);
    private static Application instance;

    public static void main(String[] args) throws IOException, LoginException, ClassNotFoundException {
        Thread.currentThread().setName("Tuner Main-Thread");
        new Application();
    }

    public static Application getApplication() {
        return instance;
    }

    private final OkHttpClient httpClient = new OkHttpClient();
    private final TunerConfiguration tunerConfiguration;
    private final ShardManager shardManager;
    private final InteractionManager interactionManager;
    private final AudioManager audioManager;

    private Application() throws IOException, LoginException, ClassNotFoundException {
        instance = this;
        this.tunerConfiguration = new TunerConfiguration(ConfigLoader.loadFileAsJson("config.json", true));

        if (tunerConfiguration.getDiscordToken() == null) {
            throw new IllegalArgumentException("config.json does not contain \"discord_token\" property!");
        }

        String token = tunerConfiguration.getDiscordToken();

        this.shardManager = DefaultShardManagerBuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES)
                .setBulkDeleteSplittingEnabled(false)
                .setGatewayEncoding(GatewayEncoding.ETF)
                .setActivity(Activity.listening("music"))
                .disableCache(CacheFlag.EMOTE)
                .setAudioSendFactory(new NativeAudioSendFactory())
                .addEventListeners(new ReadyListener(this), new SlashCommandListener(this))
                .build();

        this.interactionManager = new InteractionManager(this);
        this.audioManager = new AudioManager(this);
        Class.forName("at.xirado.tuner.db.Database");

    }

    public TunerConfiguration getTunerConfiguration() {
        return tunerConfiguration;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public InteractionManager getInteractionManager() {
        return interactionManager;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }
}

