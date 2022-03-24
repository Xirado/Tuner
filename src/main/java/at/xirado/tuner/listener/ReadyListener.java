package at.xirado.tuner.listener;

import at.xirado.tuner.Application;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    }
}
