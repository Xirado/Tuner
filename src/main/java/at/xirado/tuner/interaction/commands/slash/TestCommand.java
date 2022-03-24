package at.xirado.tuner.interaction.commands.slash;

import at.xirado.tuner.interaction.SlashCommand;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class TestCommand extends SlashCommand {

    public TestCommand() {
        super(
                Commands.slash("test", "this is a test")
                        .addOption(OptionType.STRING, "whatever", "whatever bruh")
        );

        setEnabledGuilds(815597207617142814L);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
    }
}
