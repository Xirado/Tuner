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

package at.xirado.tuner.listener;

import at.xirado.tuner.Application;
import at.xirado.tuner.audio.GuildPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ButtonListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ButtonListener.class);

    private final Application application;

    public ButtonListener(Application application) {
        this.application = application;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null)
            return;

        if (!application.getAudioManager().isGuildPlayerLoaded(event.getGuild().getIdLong())) {
            LOG.debug("Received button interaction from guild without loaded guildplayer!");
            return;
        }

        switch (event.getComponentId()) {
            case "skip" -> onSkip(event);
        }
    }

    private void onSkip(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();

        GuildPlayer guildPlayer = application.getAudioManager().getPlayer(guild);

        guildPlayer.getScheduler().nextTrack();



    }

}
