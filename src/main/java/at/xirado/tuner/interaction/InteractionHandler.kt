package at.xirado.tuner.interaction

import at.xirado.tuner.Application
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class InteractionHandler(application: Application) {

    companion object {
        val AUTOCOMPLETE_MAX_CHOICES = 25

        private val log = LoggerFactory.getLogger(InteractionHandler::class.java) as Logger
        private val commandsPackage = "at.xirado.tuner.interaction.commands"
    }

    private val globalCommands: MutableList<String> = Collections.synchronizedList(mutableListOf())




    suspend fun handleInteraction(event: GenericCommandInteractionEvent) {
        event.reply("Hello World").await()
    }
}