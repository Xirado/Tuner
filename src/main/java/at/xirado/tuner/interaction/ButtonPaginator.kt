package at.xirado.tuner.interaction

import at.xirado.tuner.Application
import at.xirado.tuner.util.await
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.await
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.internal.utils.Checks
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration

private val FIRST = Button.secondary("first", Emoji.fromUnicode("⏪"))
private val PREVIOUS = Button.secondary("previous", Emoji.fromUnicode("⬅"))
private val NEXT = Button.secondary("next", Emoji.fromUnicode("➡"))
private val LAST = Button.secondary("last", Emoji.fromUnicode("⏩"))
private val DELETE = Button.danger("stop", Emoji.fromUnicode("\uD83D\uDDD1"))

class ButtonPaginator private constructor(val timeout: Duration, val items: Array<String>, val jda: JDA, val users: Collection<Long>,
                      val itemsPerPage: Int, val numberedItems: Boolean, val title: String?, val embedColor: Int?, val footer: String?) {

    private val pages: Int = ceil(items.size / itemsPerPage.toDouble()).toInt()
    private var page = 1
    private var stopped = false

    fun paginate(messageAction: MessageAction, page: Int = 1) {
        this.page = page
        if (title == null)
            messageAction.setEmbeds(getEmbed(page)).setActionRows(getButtonLayout(page)).queue { Application.application.coroutineScope.launch { wait(it.channel.idLong, it.idLong) } }
        else
            messageAction.content(title).setEmbeds(getEmbed(page)).setActionRows(getButtonLayout(page)).queue { Application.application.coroutineScope.launch { wait(it.channel.idLong, it.idLong) } }
    }

    fun paginate(messageAction: WebhookMessageAction<Message>, page: Int = 1) {
        this.page = page
        if (title == null)
            messageAction.addEmbeds(getEmbed(page)).addActionRows(getButtonLayout(page)).queue { Application.application.coroutineScope.launch { wait(it.channel.idLong, it.idLong) } }
        else
            messageAction.setContent(title).addEmbeds(getEmbed(page)).addActionRows(getButtonLayout(page)).queue { Application.application.coroutineScope.launch { wait(it.channel.idLong, it.idLong) } }
    }

    private suspend fun wait(channelId: Long, messageId: Long) {
        val event = jda.await<ButtonInteractionEvent>(timeout) {
            if (stopped)
                return@await false

            if (messageId != it.messageIdLong)
                return@await false

            if (users.isNotEmpty())
                return@await users.contains(it.user.idLong)

            return@await true
        }

        if (event == null) {
            stopped = true
            val channel = jda.getTextChannelById(channelId) ?: return
            kotlin.runCatching {
                val message = channel.retrieveMessageById(messageId).await()
                message.editMessageComponents(emptyList()).await()
            }
        } else {
            when (event.componentId) {
                "previous" -> {
                    page--
                    if (page < 1) page = 1
                    event.editMessageEmbeds(getEmbed(page)).setActionRows(getButtonLayout(page)).queue()
                    wait(event.channel.idLong, event.messageIdLong)
                }

                "next" -> {
                    page++
                    if (page > pages) page = pages
                    event.editMessageEmbeds(getEmbed(page)).setActionRows(getButtonLayout(page)).queue()
                    wait(event.channel.idLong, event.messageIdLong)
                }

                "stop" -> {
                    stopped = true
                    if (!event.message.isEphemeral)
                        kotlin.runCatching { event.message.delete().queue() }
                    else
                        event.editMessageEmbeds(getEmbed(page)).setActionRows(emptyList()).queue()
                }

                "first" -> {
                    page = 1
                    event.editMessageEmbeds(getEmbed(page)).setActionRows(getButtonLayout(page)).queue()
                    wait(event.channel.idLong, event.messageIdLong)
                }

                "last" -> {
                    page = pages
                    event.editMessageEmbeds(getEmbed(page)).setActionRows(getButtonLayout(page)).queue()
                    wait(event.channel.idLong, event.messageIdLong)
                }
            }
        }
    }

    private fun getButtonLayout(page: Int): ActionRow {
        return if (pages > 2) {
            ActionRow.of(
                if (page <= 1) FIRST.asDisabled() else FIRST,
                if (page <= 1) PREVIOUS.asDisabled() else PREVIOUS,
                if (page >= pages) NEXT.asDisabled() else NEXT,
                if (page >= pages) LAST.asDisabled() else LAST,
                DELETE
            )
        } else {
            ActionRow.of(
                if (page <= 1) PREVIOUS.asDisabled() else PREVIOUS,
                if (page >= pages) NEXT.asDisabled() else NEXT,
                DELETE
            )
        }
    }

    private fun getEmbed(page_: Int): MessageEmbed {
        var page = if (page_ > pages) pages else page_
        if (page < 1) page = 1

        val start = if (page == 1) 0 else ((page - 1) * itemsPerPage)
        val end = min(items.size, page * itemsPerPage)
        val sb = StringBuilder()

        for (i in start until end) {
            sb.append(if (numberedItems) "**${i+1}.** " else "").append(items[i]).append("\n")
        }

        return Embed {
            footer {
                name = "Page $page/$pages" + if (footer != null) " • $footer" else ""
            }
            color = embedColor
            description = sb.toString().trim()
        }
    }

    class Builder(val jda: JDA) {


        var items: Array<String>? = null
        var numberItems = true
        var title: String? = null
        var color: Int? = null
        var footer: String? = null
        var timeout: Duration = Duration.ZERO
        var allowedUsers: Collection<Long>? = null
        var itemsPerPage = 10
            set(value) {
                Checks.check(value > 0, "Items per page must be at least 1")
                field = value
            }

        fun build(): ButtonPaginator {
            Checks.check(timeout != Duration.ZERO, "You must set a timeout using #setTimeout()!")
            Checks.noneNull(items, "Items")
            Checks.notEmpty(items, "Items")
            return ButtonPaginator(timeout, items!!, jda, allowedUsers?: emptyList(), itemsPerPage, numberItems, title, color, footer)
        }
    }
}

inline fun Paginator(jda: JDA, builder: ButtonPaginator.Builder.() -> Unit = {}) = ButtonPaginator.Builder(jda).apply(builder).build()