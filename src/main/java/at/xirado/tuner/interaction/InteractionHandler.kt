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

package at.xirado.tuner.interaction

import at.xirado.tuner.Application
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class InteractionHandler(val application: Application) {

    companion object {
        val AUTOCOMPLETE_MAX_CHOICES = 25

        private val log = LoggerFactory.getLogger(InteractionHandler::class.java) as Logger

        private const val commandsPackage = "at.xirado.tuner.interaction.commands"
    }

    private val registeredCommands = getCommandsOfClass(SlashCommand::class.java) + getCommandsOfClass(MessageContextCommand::class.java)

    private fun getCommand(name: String, type: Int, guildId: Long) : GenericCommand? {
        return registeredCommands.stream()
            .filter { it.commandData.name.equals(name, true) && ( it.isGlobal || it.enabledGuilds.contains(guildId)) && it.type == Command.Type.fromId(type) }
            .findFirst().orElse(null)
    }

    fun registerCommandsOnGuild(jda: JDA, guild: Guild) {

        val commands = registeredCommands.filter { it.isGlobal || it.enabledGuilds.contains(guild.idLong) }.map { it.commandData }
        guild.updateCommands().addCommands(commands).queue()
    }

    fun handleAutocompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.guild == null)
            return

        val guild = event.guild!!
        val command = getCommand(event.name, event.commandType.id, guild.idLong)?: return

        if (CommandFlag.DEV_ONLY in command.commandFlags && event.user.idLong !in application.tunerConfig.developers)
            return

        val missingUserPermissions = getMissingPermissions(event.member!!, event.guildChannel, command.requiredUserPermissions)
        if (missingUserPermissions.isNotEmpty())
            return

        application.coroutineScope.launch {
            when (command) {
                is SlashCommand -> command.onAutoComplete(event)
            }
        }
    }

    fun handleCommandInteraction(event: GenericCommandInteractionEvent) {
        if (event.guild == null)
            return

        val guild = event.guild!!
        val command = getCommand(event.name, event.commandType.id, guild.idLong)?: return

        if (CommandFlag.DEV_ONLY in command.commandFlags) {
            if (event.user.idLong !in application.tunerConfig.developers) {
                event.reply(":x: Only a developer can do this!").setEphemeral(true).queue()
                return
            }
        }

        val missingUserPermissions = getMissingPermissions(event.member!!, event.guildChannel, command.requiredUserPermissions)
        if (missingUserPermissions.isNotEmpty()) {
            val missingPermsString = missingUserPermissions.joinToString(", ") { "**${it}**" }
            if (missingUserPermissions.size == 1) {
                event.reply(":x: You are missing the following permission: $missingPermsString").setEphemeral(true).queue()
                return
            }
            event.reply(":x: You are missing the following permissions: $missingPermsString").setEphemeral(true).queue()
            return
        }

        val missingBotPermissions = getMissingPermissions(guild.selfMember, event.guildChannel, command.requiredBotPermissions)
        if (missingBotPermissions.isNotEmpty()) {
            val missingPermsString = missingBotPermissions.joinToString(", ") { "**${it}**" }
            if (missingBotPermissions.size == 1) {
                event.reply(":x: I am missing the following permission: $missingPermsString").setEphemeral(true).queue()
                return
            }
            event.reply(":x: I am missing the following permissions: $missingPermsString").setEphemeral(true).queue()
            return
        }

        application.coroutineScope.launch {
            when (command) {
                is SlashCommand -> command.execute(event as SlashCommandInteractionEvent)
                is MessageContextCommand -> command.execute(event as MessageContextInteractionEvent)
            }
        }
    }

    private fun getCommandsOfClass(clazz: Class<out GenericCommand>): List<GenericCommand> {
        val list = mutableListOf<GenericCommand>()

        ClassGraph().acceptPackages(commandsPackage).enableClassInfo().scan().use {
            it.getSubclasses(clazz).loadClasses().forEach { runCatching { list.add(it.getDeclaredConstructor().newInstance() as GenericCommand) } }
        }

        list.forEach { it.application = application }

        return list
    }

    private fun getMissingPermissions(member: Member, channel: GuildChannel, requiredPerms: EnumSet<Permission>) : EnumSet<Permission> {
        val perms = EnumSet.noneOf(Permission::class.java)

        for (permission in requiredPerms) {
            if (!member.hasPermission(channel, permission))
                perms.add(permission)
        }
        return perms
    }
}