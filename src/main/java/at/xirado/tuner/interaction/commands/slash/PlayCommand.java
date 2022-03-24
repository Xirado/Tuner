package at.xirado.tuner.interaction.commands.slash;

import at.xirado.tuner.audio.GuildPlayer;
import at.xirado.tuner.data.SearchHistory;
import at.xirado.tuner.interaction.InteractionManager;
import at.xirado.tuner.interaction.SlashCommand;
import at.xirado.tuner.util.Util;
import at.xirado.tuner.util.autocomplete.BasicAutocompleteChoice;
import at.xirado.tuner.util.autocomplete.IAutocompleteChoice;
import at.xirado.youtube.YoutubeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.codec.binary.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class PlayCommand extends SlashCommand {

    private static final Logger LOG = LoggerFactory.getLogger(PlayCommand.class);

    public PlayCommand() {
        super(Commands.slash("play", "plays something")
                .addOption(OptionType.STRING, "query", "what to play", true, true)
                .addOptions(new OptionData(OptionType.STRING, "provider", "Where to play from")
                        .addChoice("Youtube (Default)", "ytsearch:")
                        .addChoice("Spotify", "spsearch:")
                        .addChoice("Soundcloud", "scsearch:")
                )
        );
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Member member = event.getMember();
        GuildVoiceState voiceState = member.getVoiceState();
        AudioManager manager = event.getGuild().getAudioManager();
        if (manager.getConnectedChannel() == null) {
            try {
                manager.openAudioConnection(voiceState.getChannel());
            } catch (PermissionException exception) {
                event.getHook().sendMessage("I do not have permission to join this channel!").queue();
                return;
            }
        }
        GuildPlayer guildPlayer = getApplication().getAudioManager().getPlayer(event.getGuild());

        String query = event.getOption("query").getAsString();
        String prefix = event.getOption("provider", "ytsearch:", OptionMapping::getAsString);

        query = query.startsWith("http://") || query.startsWith("https://") ? query : prefix+query;

        getApplication().getAudioManager().getPlayerManager().loadItemOrdered(guildPlayer, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                guildPlayer.getScheduler().queue(track);
                event.getHook().sendMessage("Now playing: " + track.getInfo().title).queue();
                SearchHistory.addSearchEntry(event.getUser().getIdLong(), track.getInfo().title, track.getInfo().uri, false);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    AudioTrack single = playlist.getTracks().get(0);
                    guildPlayer.getScheduler().queue(single);
                    event.getHook().sendMessage("Now playing: " + single.getInfo().title).queue();
                    SearchHistory.addSearchEntry(event.getUser().getIdLong(), event.getOption("query").getAsString(), event.getOption("query").getAsString(), false);
                    return;
                }

                playlist.getTracks().forEach(track -> {
                    guildPlayer.getScheduler().queue(track);
                });

                SearchHistory.addSearchEntry(event.getUser().getIdLong(), playlist.getName(), event.getOption("query").getAsString(), true);

                event.getHook().sendMessage("Now playing playlist: " + playlist.getName()).queue();

            }

            @Override
            public void noMatches() {
                event.getHook().sendMessage("I didn't find anything for your search!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getHook().sendMessage("An error occurred while loading the track!").queue();
            }
        });
    }

    @Override
    public void onAutocomplete(@NotNull CommandAutoCompleteInteractionEvent event) {
        var option = event.getFocusedOption();
        if (option.getName().equals("query")) {
            String input = option.getValue();
            List<IAutocompleteChoice> choices = new ArrayList<>();

            SearchHistory.getSearchHistory(event.getUser().getIdLong(), -1)
                    .stream()
                    .filter(entry -> Util.startsWithIgnoreCase(entry.getName(), input))
                    .limit(InteractionManager.AUTOCOMPLETE_MAX_CHOICES)
                    .forEach(choices::add);

            if (!input.isEmpty() && !input.startsWith("http://") && !input.startsWith("https://") && choices.size() < InteractionManager.AUTOCOMPLETE_MAX_CHOICES) {
                try {
                    YoutubeUtil.getYoutubeMusicSearchResults(input).stream()
                            .limit(InteractionManager.AUTOCOMPLETE_MAX_CHOICES - choices.size())
                            .map(result -> new BasicAutocompleteChoice(result, result))
                            .forEach(choices::add);
                } catch (URISyntaxException | IOException e) {
                    LOG.error("Could not get Youtube Music search results!", e);
                }
            }

            event.replyChoices(choices.stream().map(IAutocompleteChoice::toJDAChoice).toList()).queue();
        }
    }
}
