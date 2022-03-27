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
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

public interface GenericCommand {

    /**
     * Gets the main application instance
     *
     * @return Main application instance
     */
    @NotNull
    Application getApplication();

    /**
     * Gets the {@link CommandData CommandData} that is sent to Discord.
     *
     * @return This commands' {@link CommandData CommandData}
     */
    @NotNull
    CommandData getCommandData();

    /**
     * Gets a {@link EnumSet EnumSet} of {@link Permission Permissions} the user is required to have to use this command.
     *
     * @return Required permissions a user needs to have to execute this command.
     */
    @NotNull
    EnumSet<Permission> getRequiredUserPermissions();

    /**
     * Gets a {@link EnumSet EnumSet} of {@link Permission Permissions} the bot is required to have to use this command.
     *
     * @return Required permissions the bot needs to have for this command to work.
     */
    @NotNull
    EnumSet<Permission> getRequiredBotPermissions();

    /**
     * Gets the type of this command.
     * <br>Possible values are:
     * <ul>
     *     <li>{@link CommandType#SLASH_COMMAND}</li>
     *     <li>{@link CommandType#USER_CONTEXT_MENU_COMMAND}</li>
     *     <li>{@link CommandType#MESSAGE_CONTEXT_MENU_COMMAND}</li>
     * </ul>
     *
     * @return The {@link CommandType CommandType} of this command.
     */
    @NotNull
    CommandType getType();

    /**
     * Whether this command is global and can be used across all guilds
     * <br>This will return true if {@link #getEnabledGuilds()} is empty, otherwise false.
     *
     * @return Whether this command is global
     */
    boolean isGlobal();

    /**
     * Returns a {@link Set Set} of Guild-IDs this command is enabled in.
     * <br>If this list is empty, the command can be used anywhere.
     *
     * @return Set of enabled Guild-IDs
     */
    @NotNull
    Set<Long> getEnabledGuilds();

    /**
     * Makes this command private (Non-public) with the specified enabled Guild-IDs.
     */
    void setEnabledGuilds(long guildId, long... guildIds);

    /**
     * Adds permissions to this command that the user executing this command needs in order to execute this command.
     */
    void addRequiredUserPermissions(Permission permission, Permission... permissions);

    /**
     * Adds permissions to this command that the bot needs in order to execute this command.
     */
    void addRequiredBotPermissions(Permission permission, Permission... permissions);

    /**
     * Sets the main application
     * @param application Main application
     */
    void setApplication(@NotNull  Application application);

    /**
     * Gets the {@link CommandFlag CommandFlags} of this command.
     * @return {@link EnumSet EnumSet} of CommandFlags this command contains.
     */
    @NotNull
    EnumSet<CommandFlag> getCommandFlags();

    /**
     * Adds {@link CommandFlag CommandFlags} to this command.
     * @param commandFlags
     */
    void addCommandFlags(CommandFlag... commandFlags);

    /**
     * Whether this command has the specified CommandFlag.
     * @param commandFlag The CommandFlag to check for
     * @return True if this command contains this CommandFlag.
     */
    boolean hasCommandFlag(CommandFlag commandFlag);
}
