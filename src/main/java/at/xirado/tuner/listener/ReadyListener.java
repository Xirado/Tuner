package at.xirado.tuner.listener;

import at.xirado.tuner.Application;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class ReadyListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ReadyListener.class);

    private final Application application;

    private boolean isReady;

    public ReadyListener(Application application) {
        this.application = application;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (isReady)
            return;

        isReady = true;
        application.getInteractionManager().init();
        registerLavalink();
    }

    private void registerLavalink() {
        DataObject config = application.getTunerConfiguration().getObject();

        DataArray nodes = config.optArray("lavalink_nodes").orElseGet(DataArray::empty);

        var lavalink = application.getLavalink();

        lavalink.setJdaProvider((shard) -> application.getShardManager().getShardById(shard));
        lavalink.setUserId(application.getShardManager().getShards().get(0).getSelfUser().getId());

        nodes.stream(DataArray::getObject).forEach(node -> {
            String url = node.getString("url");
            String password = node.getString("password");
            try {
                lavalink.addNode(new URI(url), password);
            } catch (URISyntaxException exception) {
                LOG.error("Could not add Lavalink node!", exception);
            }
        });
    }
}
