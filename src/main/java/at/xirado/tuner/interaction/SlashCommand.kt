package at.xirado.tuner.interaction

import at.xirado.tuner.Application
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.Checks
import java.util.*
import kotlin.collections.HashSet

abstract class SlashCommand(val slashCommandData: SlashCommandData) : GenericCommand {

    override lateinit var application: Application

    override val requiredUserPermissions = EnumSet.noneOf(Permission::class.java)
    override val requiredBotPermissions = EnumSet.noneOf(Permission::class.java)
    override val enabledGuilds = HashSet<Long>()
    override val commandFlags = EnumSet.noneOf(CommandFlag::class.java)

    override val commandData: CommandData
        get() = slashCommandData

    override val type: Command.Type
        get() = Command.Type.SLASH

    override val isGlobal: Boolean
        get() = enabledGuilds.isEmpty()

    override fun addRequiredUserPermissions(permission: Permission, vararg permissions: Permission) {
        Checks.notNull(permission, "Permission")
        Checks.noneNull(permissions, "Permission")

        requiredUserPermissions.add(permission)
        requiredUserPermissions.addAll(permissions.asList())
    }

    override fun addRequiredBotPermissions(permission: Permission, vararg permissions: Permission) {
        Checks.notNull(permission, "Permission")
        Checks.noneNull(permissions, "Permission")

        requiredBotPermissions.add(permission)
        requiredBotPermissions.addAll(permissions.asList())
    }

    override fun setEnabledGuilds(guildId: Long, vararg guildIds: Long) {
        enabledGuilds.clear()
        enabledGuilds.add(guildId)
        enabledGuilds.addAll(guildIds.toList())
    }

    override fun addCommandFlags(vararg commandFlags: CommandFlag) {
        Checks.noneNull(commandFlags, "CommandFlags")

        this.commandFlags.addAll(commandFlags.toList())
    }

    override fun hasCommandFlag(commandFlag: CommandFlag): Boolean {
        return this.commandFlags.contains(commandFlag)
    }

    abstract suspend fun execute(event: SlashCommandInteractionEvent)

    suspend fun onAutoComplete(event: CommandAutoCompleteInteractionEvent) {}


}