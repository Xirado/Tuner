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
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class InteractionListener(val application: Application) : ListenerAdapter() {

    override fun onGenericCommandInteraction(event: GenericCommandInteractionEvent) {
        if (!event.isFromGuild) { // This check should never fail since we don't register global commands
            event.reply("You can only execute this command from a guild!").queue()
            return
        }

        application.interactionHandler.handleCommandInteraction(event)
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (!event.isFromGuild)
            return

        application.interactionHandler.handleAutocompleteInteraction(event)
    }
}