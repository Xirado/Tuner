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

package at.xirado.tuner.interaction.commands.slash;

import at.xirado.tuner.data.guild.GuildData;
import at.xirado.tuner.interaction.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class BindCommand extends SlashCommand {

    public BindCommand() {
        super(Commands.slash("bind", "bind the bot to a textchannel")
                .addOption(OptionType.CHANNEL, "channel", "the channel to bind to")
        );
        addRequiredUserPermissions(Permission.MANAGE_CHANNEL);
        addRequiredBotPermissions(Permission.MESSAGE_MANAGE, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        GuildData guildData = getApplication().getGuildManager().getGuildDataOrCreate(event.getGuild().getIdLong());
        var channelOption = event.getOption("channel");
        if (channelOption == null) {
            event.reply("Current bound channel: <#" + guildData.getDataObject().getLong("bind_channel") + ">").queue();
            return;
        }

        var channel = channelOption.getAsGuildChannel();

        if (!(channel instanceof TextChannel)) {
            event.reply(channel.getAsMention() + " is not a TextChannel!").queue();
            return;
        }


        guildData.getDataObject().put("bind_channel", channel.getIdLong());

        guildData.update();

        event.reply("Successfully bound to " + channel.getAsMention()).queue();


    }
}
