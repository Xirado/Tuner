/*
 * Copyright 2022 Marcel Korzonek and the Tuner contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.xirado.tuner.interaction;

import at.xirado.tuner.Application;
import at.xirado.tuner.config.TunerConfiguration;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InteractionManager {

    public static final int AUTOCOMPLETE_MAX_CHOICES = 25;

    private static final Logger LOG = LoggerFactory.getLogger(InteractionManager.class);
    private static final String commandsPackage = "at.xirado.tuner.interaction.commands";

    private final Application application;

    private final List<GenericCommand> globalCommands;
    private final Map<Long, List<GenericCommand>> guildCommands;

    public InteractionManager(Application application) {
        this.application = application;
        this.globalCommands = Collections.synchronizedList(new ArrayList<>());
        this.guildCommands = new ConcurrentHashMap<>();
    }

    public void init() {
        registerCommands();
    }

    public GenericCommand getGenericCommand(String name, int type) {
        return globalCommands.stream()
                .filter(command -> command.getCommandData().getName().equalsIgnoreCase(name) && command.getType().getId() == type)
                .findFirst()
                .orElse(null);
    }

    public GenericCommand getGenericCommand(long guildId, String name, int type) {
        return guildCommands.getOrDefault(guildId, Collections.emptyList())
                .stream()
                .filter(command -> command.getCommandData().getName().equalsIgnoreCase(name) && command.getType().getId() == type)
                .findFirst()
                .orElse(getGenericCommand(name, type));
    }

    private EnumSet<Permission> getMissingPermissions(Member member, GuildChannel channel, EnumSet<Permission> requiredPerms) {
        EnumSet<Permission> perms = EnumSet.noneOf(Permission.class);

        for (Permission permission : requiredPerms) {
            if (!member.hasPermission(channel, permission))
                perms.add(permission);
        }

        return perms;
    }

    public void handleCommand(GenericCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();

        GenericCommand command = getGenericCommand(guildId, event.getName(), event.getCommandType().getId());

        if (command == null)
            return;

        Member member = event.getMember();

        var missingPermissions = getMissingPermissions(member, event.getGuildChannel(), command.getRequiredUserPermissions());

        if (!missingPermissions.isEmpty()) {
            var missingPermsString = missingPermissions.stream().map(perm -> "`" + perm + "`").collect(Collectors.joining(", "));
            event.reply("You cannot execute this command because you are missing the following permission" + (missingPermissions.size() == 1 ? "" : "s") + ":\n" + missingPermsString).setEphemeral(true).queue();
            return;
        }

        var missingBotPermissions = getMissingPermissions(event.getGuild().getSelfMember(), event.getGuildChannel(), command.getRequiredBotPermissions());

        if (!missingBotPermissions.isEmpty()) {
            var missingPermsString = missingBotPermissions.stream().map(perm -> "`" + perm + "`").collect(Collectors.joining(", "));
            event.reply("You cannot execute this command because i am missing the following permission" + (missingBotPermissions.size() == 1 ? "" : "s") + ":\n" + missingPermsString).setEphemeral(true).queue();
            return;
        }

        if (command.hasCommandFlag(CommandFlag.VOICE_CHANNEL_ONLY)) {
            var voiceState = member.getVoiceState();
            if (!voiceState.inAudioChannel()) {
                event.reply("You must be in a voice-channel to use this command!").setEphemeral(true).queue();
                return;
            }
        }

        // This only has an effect if the bot already is in a channel
        if (command.hasCommandFlag(CommandFlag.SAME_VOICE_CHANNEL_ONLY)) {
            var botVoiceState = event.getGuild().getSelfMember().getVoiceState();
            var memberVoiceState = member.getVoiceState();
            if (botVoiceState.inAudioChannel()) {
                if (!botVoiceState.getChannel().equals(memberVoiceState.getChannel())) {
                    event.reply("You must be listening in " + botVoiceState.getChannel().getAsMention() + " to do this!").setEphemeral(true).queue();
                    return;
                }
            }
        }

        switch (command.getType()) {
            case SLASH_COMMAND -> {
                var slashEvent = (SlashCommandInteractionEvent) event;
                var slashCommand = (SlashCommand) command;

                slashCommand.execute(slashEvent);
            }
        }
    }

    public void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
        SlashCommand command = (SlashCommand) getGenericCommand(event.getGuild().getIdLong(), event.getName(), CommandType.SLASH_COMMAND.getId());

        if (command == null)
            return;

        Member member = event.getMember();

        var missingPermissions = getMissingPermissions(member, event.getGuildChannel(), command.getRequiredUserPermissions());

        if (!missingPermissions.isEmpty())
            return;

        command.onAutocomplete(event);
    }

    private void registerCommands() {
        var updateAction = application.getShardManager().getShards().get(0).updateCommands();

        registerCommandsOfClass(SlashCommand.class, updateAction);

        updateAction.queue();
    }

    private void registerCommand(@NotNull CommandListUpdateAction action, @NotNull GenericCommand command) {
        TunerConfiguration config = application.getTunerConfiguration();

        Checks.notNull(command, "Command");

        command.setApplication(application);

        if (command.isGlobal() && !config.isDevMode()) {
            this.globalCommands.add(command);
            action.addCommands(command.getCommandData());
            return;
        }

        var enabledGuilds = config.isDevMode() ? config.getDevGuilds() : command.getEnabledGuilds();
        enabledGuilds.forEach(id -> addGuildCommand(id, command));
    }

    private void addGuildCommand(long guildId, GenericCommand command) {
        List<GenericCommand> enabledCommands = guildCommands.containsKey(guildId) ? guildCommands.get(guildId) : new ArrayList<>();

        if (enabledCommands.contains(command))
            throw new IllegalArgumentException(command.getType() + " \"" + command.getCommandData().getName() + "\" has already been registered!");

        enabledCommands.add(command);

        guildCommands.put(guildId, enabledCommands);
    }

    public void updateGuildCommands(Guild guild) {
        long guildId = guild.getIdLong();

        if (!guildCommands.containsKey(guildId) || guildCommands.get(guildId).isEmpty())
            return;

        var updateAction = guild.updateCommands();
        var commands = guildCommands.get(guildId);
        commands.forEach(command -> updateAction.addCommands(command.getCommandData()));
        updateAction.queue(discordCommands -> discordCommands.forEach(command -> LOG.debug("Registered command {} on guild {}", command.getName(), guildId)));
    }

    private void registerCommandsOfClass(Class<? extends GenericCommand> clazz, CommandListUpdateAction action) {
        ScanResult rs = new ClassGraph().acceptPackages(commandsPackage).enableClassInfo().scan();
        rs.getSubclasses(clazz).loadClasses().forEach(loadedClazz -> {
            try {
                registerCommand(action, (GenericCommand) loadedClazz.getDeclaredConstructor().newInstance());
                LOG.debug("Found interaction-command {}", loadedClazz.getName());
            } catch (Exception ignored) {}
        });
        rs.close();
    }

    public Map<Long, List<GenericCommand>> getGuildCommands() {
        return Collections.unmodifiableMap(guildCommands);
    }
}
