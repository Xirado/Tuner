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
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.Commands
import java.util.*

abstract class MessageContextCommand(name: String, devCommand: Boolean = false) : GenericCommand {

    final override val commandData = Commands.message(name)
    final override val requiredUserPermissions = EnumSet.noneOf(Permission::class.java)
    final override val requiredBotPermissions = EnumSet.noneOf(Permission::class.java)
    final override val enabledGuilds = HashSet<Long>()
    final override val commandFlags = EnumSet.noneOf(CommandFlag::class.java)
    final override val application: Application
        get() = Application.application

    override val type: Command.Type
        get() = Command.Type.MESSAGE

    override val isGlobal: Boolean
        get() = enabledGuilds.isEmpty()

    init {
        if (devCommand) {
            enabledGuilds.addAll(application.tunerConfig.devGuilds)
            commandFlags.add(CommandFlag.DEV_ONLY)
        }
    }

    abstract suspend fun execute(event: MessageContextInteractionEvent)
}