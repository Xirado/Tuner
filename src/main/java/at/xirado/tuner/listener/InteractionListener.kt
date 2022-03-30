package at.xirado.tuner.listener

import at.xirado.tuner.Application
import at.xirado.tuner.application
import dev.minn.jda.ktx.await
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class InteractionListener(application: Application) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        application.coroutineScope.launch {
            if (!event.isFromGuild) {
                event.reply("You can only execute this command from a guild!").await()
                return@launch
            }
            application.interactionHandler.handleCommand(event)
        }
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {

    }
}