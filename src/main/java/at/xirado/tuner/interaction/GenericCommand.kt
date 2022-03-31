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
import java.util.EnumSet
import at.xirado.tuner.interaction.CommandFlag
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData

interface GenericCommand {
    var application: Application
    val commandData: CommandData
    val requiredUserPermissions: EnumSet<Permission>
    val requiredBotPermissions: EnumSet<Permission>
    val type: Command.Type
    val isGlobal: Boolean
    val enabledGuilds: Set<Long>
    fun setEnabledGuilds(guildId: Long, vararg guildIds: Long)
    fun addRequiredUserPermissions(permission: Permission, vararg permissions: Permission)
    fun addRequiredBotPermissions(permission: Permission, vararg permissions: Permission)
    val commandFlags: EnumSet<CommandFlag>
    fun addCommandFlags(vararg commandFlags: CommandFlag)
    fun hasCommandFlag(commandFlag: CommandFlag): Boolean
}