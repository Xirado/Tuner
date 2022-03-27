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
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadyListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ReadyListener.class);

    private final Application application;

    private boolean isReady;

    public ReadyListener(Application application) {
        this.application = application;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (!isReady) {
            application.getInteractionManager().init();
            isReady = true;
        }

        application.getInteractionManager().updateGuildCommands(event.getGuild());
    }
}
