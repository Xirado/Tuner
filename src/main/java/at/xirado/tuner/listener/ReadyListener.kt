package at.xirado.tuner.listener

import at.xirado.tuner.Application
import at.xirado.tuner.application
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class ReadyListener(application: Application) : ListenerAdapter() {

    private var ready = false

    override fun onGuildReady(event: GuildReadyEvent) {
        if (!ready) {
            application.interactionHandler.init()
            ready = true
        }
        application.interactionHandler.updateGuildCommands(event.guild)
    }
}