package at.xirado.tuner.interaction.commands.slash

import at.xirado.tuner.Application
import at.xirado.tuner.interaction.SlashCommand
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.getOption
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.awt.Color

class BannerCommand : SlashCommand("banner", "get someones banner")  {

    init {
        option<User>(name = "user", description = "the user to get the banner from")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val user = event.getOption<User>("user")?: event.user

        val profile = user.retrieveProfile().await()

        val embed = Embed {
            title = "${user.asTag}'s banner"
            image = "${profile.bannerUrl}?size=1024"
            color = Color.magenta.rgb
        }

        event.hook.sendMessageEmbeds(embed).queue()
    }
}