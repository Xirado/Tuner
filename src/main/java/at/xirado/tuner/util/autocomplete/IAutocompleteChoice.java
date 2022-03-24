package at.xirado.tuner.util.autocomplete;

import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

public interface IAutocompleteChoice {

    @NotNull
    String getName();

    @NotNull
    String getValue();

    @NotNull
    Command.Choice toJDAChoice();
}
