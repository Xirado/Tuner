package at.xirado.tuner.util

import at.xirado.tuner.Application
import at.xirado.tuner.data.GuildData
import dev.minn.jda.ktx.Embed
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

suspend inline fun SlashCommandInteractionEvent.getGuildData(): GuildData {
    if (!isFromGuild)
        throw IllegalStateException("This event does not originate from a guild!")

    return Application.application.guildManager.getGuildData(guild!!.idLong)
}

fun GenericCommandInteractionEvent.replyError(message: String, ephemeral: Boolean = false): ReplyCallbackAction {
    return replyEmbeds(Embed {
        description = "❌ $message"
        color = 0xde3623
    }).setEphemeral(ephemeral)
}