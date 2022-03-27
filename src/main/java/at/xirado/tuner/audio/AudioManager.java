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
import com.github.topislavalinkplugins.topissourcemanagers.spotify.SpotifyConfig;
import com.github.topislavalinkplugins.topissourcemanagers.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AudioManager {

    private static final Logger LOG = LoggerFactory.getLogger(AudioManager.class);

    private final Application application;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildPlayer> audioPlayers;

    public AudioManager(Application application) {
        this.application = application;
        this.playerManager = new DefaultAudioPlayerManager();

        // Register spotify source manager
        if (application.getTunerConfiguration().getSpotifyClientId() != null && application.getTunerConfiguration().getSpotifyClientSecret() != null) {
            SpotifyConfig spotifyConfig = new SpotifyConfig();
            spotifyConfig.setClientId(application.getTunerConfiguration().getSpotifyClientId());
            spotifyConfig.setClientSecret(application.getTunerConfiguration().getSpotifyClientSecret());
            spotifyConfig.setCountryCode("US");
            playerManager.registerSourceManager(new SpotifySourceManager(null, spotifyConfig, this.playerManager));
            LOG.info("Registered SpotifySourceManager");
        } else {
            LOG.error("Could not register SpotifySourceManager! Missing credentials");
        }


        this.audioPlayers = new ConcurrentHashMap<>();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    public synchronized boolean isGuildPlayerLoaded(long guildId) {
        return audioPlayers.containsKey(guildId);
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
