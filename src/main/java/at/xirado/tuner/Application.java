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

package at.xirado.tuner;

import at.xirado.tuner.audio.AudioManager;
import at.xirado.tuner.config.ConfigLoader;
import at.xirado.tuner.config.TunerConfiguration;
import at.xirado.tuner.data.guild.GuildManager;
import at.xirado.tuner.interaction.InteractionManager;
import at.xirado.tuner.listener.ReadyListener;
import at.xirado.tuner.listener.SlashCommandListener;
import ch.qos.logback.classic.Level;
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
import java.util.Arrays;
import java.util.List;

public class Application {

    private static Logger LOG = LoggerFactory.getLogger(Application.class);
    private static Application instance;

    public static void main(String[] args) throws IOException, LoginException, ClassNotFoundException {
        List<String> argsList = Arrays.asList(args);
        if (!argsList.contains("--noclear")) {
            System.out.print("\033[2J\033[H"); // Clears the terminal
        }

        if (argsList.contains("--debug")) {
            LOG.info("Started with \"--debug\" argument.");
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ROOT")).setLevel(Level.DEBUG);
        }

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
    private final GuildManager guildManager;

    private Application() throws IOException, LoginException, ClassNotFoundException {
        instance = this;
        this.tunerConfiguration = new TunerConfiguration(ConfigLoader.loadFileAsYaml("config.yml", true));

        if (tunerConfiguration.getDiscordToken() == null || tunerConfiguration.getDiscordToken().isEmpty()) {
            throw new IllegalArgumentException("config.yml does not contain \"discord_token\" property!");
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
        this.guildManager = new GuildManager(shardManager);

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

    public GuildManager getGuildManager() {
        return guildManager;
    }
}

