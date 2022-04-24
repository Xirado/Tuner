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
import dev.minn.jda.ktx.interactions.Subcommand
import dev.minn.jda.ktx.interactions.optionType
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.internal.utils.Checks
import java.util.*

abstract class SlashCommand(name: String, description: String, devCommand: Boolean = false) : GenericCommand {

    final override lateinit var application: Application

    final override val commandData = Commands.slash(name, description)
    final override val requiredUserPermissions = EnumSet.noneOf(Permission::class.java)
    final override val requiredBotPermissions = EnumSet.noneOf(Permission::class.java)
    final override val enabledGuilds = HashSet<Long>()
    final override val commandFlags = EnumSet.noneOf(CommandFlag::class.java)

    init {
        if (devCommand)
            enabledGuilds.addAll(application.tunerConfig.devGuilds)
    }

    override val type: Command.Type
        get() = Command.Type.SLASH

    override val isGlobal: Boolean
        get() = enabledGuilds.isEmpty()

    inline fun <reified T> option(name: String, description: String, required: Boolean = false, autocomplete: Boolean = false, builder: OptionData.() -> Unit = {}) {
        val type = optionType<T>()
        if (type == OptionType.UNKNOWN)
            throw IllegalArgumentException("Cannot resolve type " + T::class.java.simpleName + " to OptionType!")

        commandData.addOptions(OptionData(type, name, description).setRequired(required).setAutoComplete(autocomplete).apply(builder))
    }

    inline fun subcommand(name: String, description: String, builder: SubcommandData.() -> Unit = {}) = commandData.addSubcommands(
        Subcommand(name, description, builder)
    )

    abstract suspend fun execute(event: SlashCommandInteractionEvent)

    open suspend fun onAutoComplete(event: CommandAutoCompleteInteractionEvent) {}


}