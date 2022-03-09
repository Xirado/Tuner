package at.xirado.tuner.interaction.commands.slash;

import at.xirado.tuner.audio.GuildPlayer;
import at.xirado.tuner.interaction.SlashCommand;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.io.jda.JdaLink;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;


public class PlayCommand extends SlashCommand {
    public PlayCommand() {
        super(Commands.slash("play", "plays something")
                .addOption(OptionType.STRING, "query", "what to play", true, false)
                .addOptions(new OptionData(OptionType.STRING, "provider", "Where to play from")
                        .addChoice("Youtube (Default)", "ytsearch:")
                        .addChoice("Spotify", "spsearch:")
                        .addChoice("Soundcloud", "scsearch:")
                )
        );
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        JdaLink link = getApplication().getLavalink().getLink(event.getGuild());
        event.deferReply().queue();
        Member member = event.getMember();
        GuildVoiceState voiceState = member.getVoiceState();
        AudioManager manager = event.getGuild().getAudioManager();
        if (manager.getConnectedChannel() == null) {
            try {
                link.connect(voiceState.getChannel());
            } catch (PermissionException exception) {
                event.getHook().sendMessage("I do not have permission to join this channel!").queue();
                return;
            }
        }
        GuildPlayer guildPlayer = getApplication().getAudioManager().getPlayer(event.getGuild().getIdLong());

        String query = event.getOption("query").getAsString();
        String prefix = event.getOption("provider", "ytsearch:", OptionMapping::getAsString);

        query = query.startsWith("http://") || query.startsWith("https://") ? query : prefix+query;

        link.getRestClient().loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                guildPlayer.getScheduler().queue(track);
                event.getHook().sendMessage("Now playing: " + track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    AudioTrack single = playlist.getTracks().get(0);
                    guildPlayer.getScheduler().queue(single);
                    event.getHook().sendMessage("Now playing: " + single.getInfo().title).queue();
                    return;
                }

                playlist.getTracks().forEach(track -> {
                    guildPlayer.getScheduler().queue(track);
                });

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
}
