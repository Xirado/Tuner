package at.xirado.tuner.listener;

import at.xirado.tuner.Application;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

public class SlashCommandListener extends ListenerAdapter {

    private final Application application;

    public SlashCommandListener(@NotNull Application application) {
        Checks.notNull(application, "Application");
        this.application = application;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        application.getInteractionManager().handleInteraction(event);
    }
}
