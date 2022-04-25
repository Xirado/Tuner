package at.xirado.tuner.interaction.commands.message.dev

import at.xirado.tuner.interaction.MessageContextCommand
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent

class MentionsTestCommand : MessageContextCommand("mentions", devCommand = true) {

    override suspend fun execute(event: MessageContextInteractionEvent) {
        val mentions = event.target.mentionedMembers

        event.reply(mentions.joinToString("\n") { it.asMention }).queue()
    }


}