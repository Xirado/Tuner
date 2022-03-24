package at.xirado.tuner.util.autocomplete;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

public class SearchHistoryAutocompleteChoice implements IAutocompleteChoice{

    public static final String SEARCH_INDICATOR = "\uD83D\uDD52";
    public static final String PLAYLIST_INDICATOR = "\uD83D\uDCDC"; // indicates that this is a playlist

    private final String name;
    private final String value;
    private final long searchedOn;
    private final boolean playlist;

    public SearchHistoryAutocompleteChoice(String name, String value, long searchedOn, boolean playlist) {
        Checks.notNull(name, "Name");
        Checks.notNull(value, "Value");

        this.name = name;
        this.value = value;
        this.searchedOn = searchedOn;
        this.playlist = playlist;
    }

    public boolean isPlaylist() {
        return playlist;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return searchedOn;
    }

    @NotNull
    public String getFormattedString()
    {
        String x = SEARCH_INDICATOR + (playlist ? PLAYLIST_INDICATOR : "") + " " + name;
        if (x.length() > 100)
        {
            String replaceString = "...";
            String substr = x.substring(0, 100 - replaceString.length());
            x = substr + replaceString;
        }
        return x;
    }

    @Override
    @NotNull
    public Command.Choice toJDAChoice() {
        return new Command.Choice(getFormattedString(), value);
    }
}
