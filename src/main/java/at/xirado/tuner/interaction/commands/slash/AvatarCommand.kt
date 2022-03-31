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

package at.xirado.tuner.interaction.commands.slash

import at.xirado.tuner.interaction.SlashCommand
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.getOption
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType.*
import java.awt.Color

class AvatarCommand : SlashCommand("avatar", "gets the avatar from a user") {

    init {
        option(type = USER, name = "user", description = "the user to get the avatar from")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val user = event.getOption<User>("user")?: event.user
        val embed = Embed {
            title = "${user.asTag}'s avatar"
            image = user.avatarUrl
            color = Color.magenta.rgb
        }

        event.replyEmbeds(embed).await()
    }
}