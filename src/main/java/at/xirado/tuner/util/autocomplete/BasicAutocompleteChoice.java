package at.xirado.tuner.util.autocomplete;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

public class BasicAutocompleteChoice implements IAutocompleteChoice {

    private final String name;
    private final String value;

    public BasicAutocompleteChoice(String name, String value) {
        Checks.notNull(name, "Name");
        Checks.notNull(value, "Value");
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getValue() {
        return value;
    }

    @NotNull
    @Override
    public Command.Choice toJDAChoice() {
        return new Command.Choice(name, value);
    }
}
