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

package at.xirado.tuner.listener

import at.xirado.tuner.Application
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*

class RegistrationListener(val application: Application) : ListenerAdapter() {

    companion object {
        private val readyGuilds = Collections.synchronizedList<Long>(mutableListOf())
    }

    override fun onGuildReady(event: GuildReadyEvent) {
        if (event.guild.idLong !in readyGuilds) {
            readyGuilds.add(event.guild.idLong)
            application.coroutineScope.launch {
                val commandBot = application.multiBotManager.getBotWithRegisteredCommands(event.guild.idLong)
                if (commandBot == null) {
                    application.interactionHandler.registerCommandsOnGuild(event.guild)
                } else {
                    application.interactionHandler.registerCommandsOnGuild(commandBot.getGuildById(event.guild.idLong)!!)
                }
            }
        }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        val botCount = application.multiBotManager.getBotCount(event.guild.idLong)
        if (botCount == 1) {
            application.interactionHandler.registerCommandsOnGuild(event.guild)
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        val botCount = application.multiBotManager.getBotCount(event.guild.idLong)
        if (botCount > 0) {
            val bot = application.multiBotManager.getBotsInGuild(event.guild.idLong)[0]
            application.coroutineScope.launch {
                val commandBot = application.multiBotManager.getBotWithRegisteredCommands(event.guild.idLong)
                if (commandBot == null) {
                    application.interactionHandler.registerCommandsOnGuild(bot.getGuildById(event.guild.idLong)!!)
                }
            }
        }
    }
}