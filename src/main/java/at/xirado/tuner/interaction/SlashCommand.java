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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class SlashCommand implements GenericCommand {

    private Application application;

    private final SlashCommandData commandData;
    private final EnumSet<Permission> requiredUserPermissions;
    private final EnumSet<Permission> requiredBotPermissions;
    private final Set<Long> enabledGuilds;
    private final EnumSet<CommandFlag> commandFlags;

    public SlashCommand(SlashCommandData commandData) {
        this.commandData = commandData;
        this.requiredUserPermissions = EnumSet.noneOf(Permission.class);
        this.requiredBotPermissions = EnumSet.noneOf(Permission.class);
        this.enabledGuilds = new HashSet<>();
        this.commandFlags = EnumSet.noneOf(CommandFlag.class);
    }

    @Override
    @NotNull
    public CommandData getCommandData() {
        return commandData;
    }

    @Override
    @NotNull
    public EnumSet<Permission> getRequiredUserPermissions() {
        return requiredUserPermissions;
    }

    @Override
    @NotNull
    public EnumSet<Permission> getRequiredBotPermissions() {
        return requiredBotPermissions;
    }

    @Override
    @NotNull
    public CommandType getType() {
        return CommandType.SLASH_COMMAND;
    }

    @Override
    public boolean isGlobal() {
        return enabledGuilds.isEmpty();
    }

    @Override
    @NotNull
    public Set<Long> getEnabledGuilds() {
        return enabledGuilds;
    }

    @NotNull
    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public void setEnabledGuilds(long guildId, long... guildIds) {
        enabledGuilds.clear();
        enabledGuilds.add(guildId);
        Arrays.stream(guildIds).forEach(enabledGuilds::add);
    }

    @Override
    public void addRequiredUserPermissions(Permission permission, Permission... permissions) {
        Checks.notNull(permission, "Permission");
        Checks.noneNull(permissions, "Permissions");

        requiredUserPermissions.add(permission);
        requiredUserPermissions.addAll(List.of(permissions));
    }

    @Override
    public void addRequiredBotPermissions(Permission permission, Permission... permissions) {
        Checks.notNull(permission, "Permission");
        Checks.noneNull(permissions, "Permissions");

        requiredBotPermissions.add(permission);
        requiredBotPermissions.addAll(List.of(permissions));
    }

    @Override
    public void setApplication(@NotNull Application application) {
        this.application = application;
    }

    public abstract void execute(@NotNull SlashCommandInteractionEvent event);

    public void onAutocomplete(@NotNull CommandAutoCompleteInteractionEvent event) {}

    @Override
    public void addCommandFlags(CommandFlag... commandFlags) {
        Checks.noneNull(commandFlags, "CommandFlags");
        this.commandFlags.addAll(List.of(commandFlags));
    }

    @Override
    public @NotNull EnumSet<CommandFlag> getCommandFlags() {
        return EnumSet.copyOf(this.commandFlags);
    }

    @Override
    public boolean hasCommandFlag(CommandFlag commandFlag) {
        Checks.notNull(commandFlag, "CommandFlag");
        return this.commandFlags.contains(commandFlag);
    }
}
