package at.xirado.tuner.config;

import club.minnced.discord.webhook.WebhookClient;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import club.minnced.discord.webhook.WebhookClientBuilder;

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
    private final String innertubeRequestBody;

    public TunerConfiguration(DataObject object) {
        this.object = object;

        this.discordToken = object.getString("discord_token", null);

        this.devMode = object.getBoolean("dev_mode", false);

        this.devGuilds = object.optArray("dev_guilds").orElseGet(DataArray::empty)
                .stream(DataArray::getLong)
                .collect(Collectors.toUnmodifiableSet());

        if (this.devMode && this.devGuilds.isEmpty()) {
            LOG.warn("Dev mode enabled but no development guilds set!");
        }

        this.webhookClient = object.isNull("webhook_url") ? null : new WebhookClientBuilder(object.getString("webhook_url")).build();
        this.innertubeApiKey = object.getString("innertube_api_key", null);
        this.innertubeRequestBody = object.isNull("innertube_request_body") ? null : object.getObject("innertube_request_body").toString();
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

    public String getInnertubeRequestBody() {
        return innertubeRequestBody;
    }
}
