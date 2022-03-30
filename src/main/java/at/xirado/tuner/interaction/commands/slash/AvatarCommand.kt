package at.xirado.tuner.interaction.commands.slash

import at.xirado.tuner.interaction.SlashCommand
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.getOption
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import java.awt.Color

class AvatarCommand : SlashCommand(
    Commands.slash("avatar", "gets the avatar from a user")
        .addOption(OptionType.USER, "user", "the user to get the avatar from")
) {

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val user = event.getOption<User>("user")?: event.user

        val embed = Embed {
            title = "${user.asTag}'s avatar"
            image = user.avatarUrl
            color = Color.magenta.rgb
        }
        event.replyEmbeds(embed).await()
    }
}