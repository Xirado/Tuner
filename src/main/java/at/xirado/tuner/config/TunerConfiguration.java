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

package at.xirado.tuner.config;

import club.minnced.discord.webhook.WebhookClient;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import club.minnced.discord.webhook.WebhookClientBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.stream.Collectors;

public class TunerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TunerConfiguration.class);

    private final DataObject object;

    private final String discordToken;
    private final boolean devMode;
    private final Set<Long> devGuilds;
    private final WebhookClient webhookClient;
    private final String innertubeApiKey;
    private final String innertubeRequestBodyLocation;

    private String innertubeRequestBody;
    private String spotifyClientId;
    private String spotifyClientSecret;

    public TunerConfiguration(DataObject object) {
        this.object = object;

        this.discordToken = getString(object, "discord_token");

        this.devMode = object.getBoolean("dev_mode", false);

        this.devGuilds = object.optArray("dev_guilds").orElseGet(DataArray::empty)
                .stream(DataArray::getLong)
                .collect(Collectors.toUnmodifiableSet());

        if (this.devMode && this.devGuilds.isEmpty()) {
            LOG.warn("Dev mode enabled but no development guilds specified in config. You will not see any commands!");
        }

        this.webhookClient = object.isNull("webhook_url") || object.getString("webhook_url").isEmpty() ? null : new WebhookClientBuilder(object.getString("webhook_url")).build();

        var ytConfig = object.optObject("youtube").orElseGet(DataObject::empty);

        this.innertubeApiKey = getString(ytConfig, "innertube_api_key");

        this.innertubeRequestBodyLocation = getString(ytConfig, "innertube_request_body_location");

        if (this.innertubeRequestBodyLocation != null) {
            File file = new File(this.innertubeRequestBodyLocation);

            if (file.exists()) {
                try {
                    this.innertubeRequestBody = DataObject.fromJson(new FileInputStream(file)).toString();
                } catch (FileNotFoundException ignored) {

                } catch (ParsingException exception) {
                    LOG.error("Could not parse Innertube request-body JSON!", exception);
                }

            }
        }

        if (getString(ytConfig, "innertube_papisid") != null)
            YoutubeHttpContextFilter.setPAPISID(ytConfig.getString("innertube_papisid"));

        if (getString(ytConfig, "innertube_psid") != null)
            YoutubeHttpContextFilter.setPSID(ytConfig.getString("innertube_psid"));

        var spotifyConfig = object.optObject("spotify").orElseGet(DataObject::empty);

        if (getString(spotifyConfig, "client_id") != null)
            this.spotifyClientId = spotifyConfig.getString("client_id", null);

        if (getString(spotifyConfig, "client_secret") != null)
            this.spotifyClientSecret = spotifyConfig.getString("client_secret", null);
    }

    private String getString(DataObject object, String key) {
        if (object.isNull(key))
            return null;

        var string = object.getString(key);

        if (string.isBlank())
            return null;

        return string;
    }

    public DataObject getObject() {
        return object;
    }

    public String getDiscordToken() {
        return discordToken;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public Set<Long> getDevGuilds() {
        return devGuilds;
    }

    public WebhookClient getWebhookClient() {
        return webhookClient;
    }

    public String getInnertubeApiKey() {
        return innertubeApiKey;
    }

    public String getInnertubeRequestBodyLocation() {
        return innertubeRequestBodyLocation;
    }

    public String getInnertubeRequestBody() {
        return innertubeRequestBody;
    }

    public String getSpotifyClientId() {
        return spotifyClientId;
    }

    public String getSpotifyClientSecret() {
        return spotifyClientSecret;
    }
}