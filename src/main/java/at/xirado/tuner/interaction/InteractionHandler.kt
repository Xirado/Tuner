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
import at.xirado.tuner.application
import io.github.classgraph.ClassGraph
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import net.dv8tion.jda.internal.utils.Checks
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InteractionHandler(application: Application) {

    companion object {
        val AUTOCOMPLETE_MAX_CHOICES = 25

        private val log = LoggerFactory.getLogger(InteractionHandler::class.java) as Logger
        private val commandsPackage = "at.xirado.tuner.interaction.commands"
    }

    private val globalCommands: MutableList<GenericCommand> = Collections.synchronizedList(mutableListOf())
    private val guildCommands: MutableMap<Long, MutableList<GenericCommand>> = ConcurrentHashMap()

    fun init() {
        registerCommands()
    }

    suspend fun handleCommand(event: GenericCommandInteractionEvent) {
        val guild = event.guild!!

        val guildId = guild.idLong

        val command = getGenericCommand(guildId, event.name, event.commandType.id)?: return

        val member = event.member

        when (command.type) {
            Command.Type.SLASH -> {
                val slashEvent = event as SlashCommandInteractionEvent
                val slashCommand = command as SlashCommand
                slashCommand.execute(slashEvent)
            }
        }
    }

    private fun registerCommands() {
        val updateAction = application.shardManager.shards[0].updateCommands()

        registerCommandsOfClass(SlashCommand::class.java, updateAction)

        updateAction.queue()
    }

    private fun registerCommand(action: CommandListUpdateAction, command: GenericCommand) {
        val config = application.tunerConfig

        Checks.notNull(command, "Command")

        command.application = application

        if (command.isGlobal && !config.isDevMode) {
            globalCommands.add(command)
            action.addCommands(command.commandData)
            return
        }

        val enabledGuilds = if (config.isDevMode) config.devGuilds else command.enabledGuilds
        enabledGuilds.forEach { addGuildCommand(it, command) }
    }

    private fun addGuildCommand(guildId: Long, command: GenericCommand) {
        val enabledCommands = if (guildCommands.containsKey(guildId)) guildCommands[guildId] else mutableListOf()

        if (command in enabledCommands!!)
            throw IllegalArgumentException("${command.type} ${command.commandData.name} has already been registered!")

        enabledCommands.add(command)

        guildCommands.put(guildId, enabledCommands)
    }

    fun updateGuildCommands(guild: Guild) {
        val guildId = guild.idLong

        if (guildId !in guildCommands || guildCommands[guildId]!!.isEmpty())
            return

        val updateAction = guild.updateCommands()

        val commands = guildCommands[guildId]!!

        commands.forEach { updateAction.addCommands(it.commandData) }

        updateAction.queue {
            it.forEach {  command ->
                log.debug("Registered command ${command.name} on guild $guildId")
            }
        }
    }

    private fun registerCommandsOfClass(clazz: Class<out GenericCommand>, action: CommandListUpdateAction) {
        val scanResult = ClassGraph().acceptPackages(commandsPackage).enableClassInfo().scan()

        scanResult.getSubclasses(clazz).loadClasses().forEach {
            try {
                registerCommand(action, it.getDeclaredConstructor().newInstance() as GenericCommand)
                log.debug("Found interaction-command ${it.name}")
            } catch (ignored: Exception) {}
        }
        scanResult.close()
    }

    fun getGuildCommands() : Map<Long, List<GenericCommand>> {
        return Collections.unmodifiableMap(guildCommands)
    }

    fun getGenericCommand(name: String, type: Int) : GenericCommand? {
        return globalCommands.stream()
            .filter { it.commandData.name.equals(name, true) && it.type.id == type}
            .findFirst().orElse(null)
    }

    fun getGenericCommand(guildId: Long, name: String, type: Int) : GenericCommand? {
        return guildCommands.getOrDefault(guildId, listOf())
            .stream()
            .filter { it.commandData.name.equals(name, true) && it.type.id == type }
            .findFirst()
            .orElse(getGenericCommand(name, type))
    }
}