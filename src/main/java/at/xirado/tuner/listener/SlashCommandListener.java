package at.xirado.tuner.listener;

import at.xirado.tuner.Application;
import at.xirado.tuner.interaction.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class SlashCommandListener extends ListenerAdapter {

    private final Application application;

    public SlashCommandListener(@NotNull Application application) {
        Checks.notNull(application, "Application");
        this.application = application;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("You can only execute this command from a guild!").queue();
            return;
        }
        application.getInteractionManager().handleCommand(event);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.replyChoices(Collections.emptyList()).queue();
            return;
        }
        application.getInteractionManager().handleAutocomplete(event);
    }
}
