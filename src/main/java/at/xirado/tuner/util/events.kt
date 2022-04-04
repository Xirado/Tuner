package at.xirado.tuner.util

import at.xirado.tuner.Application
import at.xirado.tuner.data.GuildData
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

suspend inline fun SlashCommandInteractionEvent.getGuildData(): GuildData {
    if (!isFromGuild)
        throw IllegalStateException("This event does not originate from a guild!")

    return Application.application.guildManager.getGuildData(guild!!.idLong)
}