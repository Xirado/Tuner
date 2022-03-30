package at.xirado.tuner.interaction

import at.xirado.tuner.Application
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*
import kotlin.collections.HashSet

abstract class SlashCommand(data: SlashCommandData) : GenericCommand {

    override lateinit var application: Application

    override val requiredUserPermissions = EnumSet.noneOf(Permission::class.java)
    override val requiredBotPermissions = EnumSet.noneOf(Permission::class.java)
    override val enabledGuilds = HashSet<Long>()
    override val commandFlags = EnumSet.noneOf(CommandFlag::class.java)

}