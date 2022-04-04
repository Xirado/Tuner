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

package at.xirado.tuner.audio;

import at.xirado.tuner.Application;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class GuildPlayer {

    private final Application application;
    private final AudioPlayer player;
    private final AudioScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;
    private final long guildId;

    public GuildPlayer(Application application, long guildId, AudioPlayerManager audioPlayerManager) {
        this.application = application;
        this.guildId = guildId;
        this.player = audioPlayerManager.createPlayer();
        this.sendHandler = new AudioPlayerSendHandler(player);
        this.scheduler = new AudioScheduler(application, player, guildId, this);
        player.addListener(scheduler);
    }

    public AudioScheduler getScheduler() {
        return scheduler;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public long getGuildId() {
        return guildId;
    }

    public Application getApplication() {
        return application;
    }

    public AudioPlayerSendHandler getSendHandler() {
        return sendHandler;
    }

    public void destroy() {
        player.destroy();
        scheduler.getQueue().clear();
        application.getAudioManager().destroy(this);
    }
}