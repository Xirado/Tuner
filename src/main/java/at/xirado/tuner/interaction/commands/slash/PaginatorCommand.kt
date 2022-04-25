package at.xirado.tuner.interaction.commands.slash

import at.xirado.tuner.Application
import at.xirado.tuner.interaction.SlashCommand
import dev.minn.jda.ktx.interactions.getOption
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.time.Duration.Companion.minutes
import at.xirado.tuner.interaction.Paginator

class PaginatorCommand : SlashCommand("paginator", "tests the button paginator", devCommand = true) {

    init {
        option<Boolean>(name = "ephemeral", description = "Whether the response should be ephemeral")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(event.getOption<Boolean>("ephemeral")?: false).queue()

        val paginator = Paginator(event.jda) {
            itemsPerPage = 2
            color = 0x4287f5
            timeout = 1.minutes
            title = "Lorem ipsum dolor sit amet consetetur sadipscing elitr sed diam"
            footer = "Lorem ipsum dolor sit amet consetetur sadipscing elitr sed diam"
            allowedUsers = listOf(event.user.idLong)
            items = arrayOf(
                "Lorem ipsum dolor sit amet consetetur sadipscing elitr sed diam",
                "nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam",
                "erat sed diam voluptua. At vero eos et accusam et",
                "justo duo dolores et ea rebum. Stet clita kasd gubergren",
                "no sea takimata sanctus est Lorem ipsum dolor sit amet.",
                "Lorem ipsum dolor sit amet consetetur sadipscing elitr sed diam",
                "nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam",
                "erat sed diam voluptua. At vero eos et accusam et",
                "justo duo dolores et ea rebum. Stet clita kasd gubergren",
                "no sea takimata sanctus est Lorem ipsum dolor sit amet.",
                "Lorem ipsum dolor sit amet consetetur sadipscing elitr sed diam",
                "nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam",
                "erat sed diam voluptua. At vero eos et accusam et"
            )
        }

        paginator.paginate(event.hook.sendMessage(""), 1)
    }
}