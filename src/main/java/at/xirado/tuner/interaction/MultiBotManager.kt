package at.xirado.tuner.interaction

import at.xirado.tuner.Application
import at.xirado.tuner.audio.AudioManager
import dev.minn.jda.ktx.await
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.internal.JDAImpl
import net.dv8tion.jda.internal.interactions.command.CommandImpl
import net.dv8tion.jda.internal.requests.RestActionImpl
import net.dv8tion.jda.internal.requests.Route
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MultiBotManager(val application: Application) {

    fun getBotCount(guildId: Long): Int {
        return application.bots.values.stream().filter { it.getGuildById(guildId) != null }.count().toInt()
    }

    fun getBotsInGuild(guildId: Long): List<JDA> {
        return application.bots.values.stream().filter { it.getGuildById(guildId) != null }.toList()
    }

    fun getAudioManagers(guildId: Long): List<AudioManager> = getBotsInGuild(guildId).map { application.audioManagers[it.selfUser.idLong]!! }

    fun isBotAvailable(guildId: Long) = getBotsInGuild(guildId).any { !it.getGuildById(guildId)!!.audioManager.isConnected }

    fun getAvailableBots(guildId: Long) = getBotsInGuild(guildId).filter { !it.getGuildById(guildId)!!.audioManager.isConnected }.toList()

    fun getConnectedBots(guildId: Long, channelId: Long): List<JDA> {
        val bots = getBotsInGuild(guildId)
        return bots.filter {
            val guild = it.getGuildById(guildId)!!

            val channel = guild.audioManager.connectedChannel?: return@filter false

            return@filter channel.idLong == channelId
        }
    }

    suspend fun getBotWithRegisteredCommands(guildId: Long): JDA? {
        return suspendCoroutine { continuation ->
            application.coroutineScope.launch {
                supervisorScope {
                    val map = mutableMapOf<JDA, Deferred<DataArray>>()

                    application.bots.values.associateWith { bot ->
                        map[bot] = async {
                            withContext(Dispatchers.IO) {
                                retrieveCommands(bot, guildId).await()
                            }
                        }
                    }
                    map.entries.forEach {
                        val array = kotlin.runCatching { it.value.await() }.onFailure { return@forEach }.getOrThrow()

                        if (!array.isEmpty) {
                            continuation.resume(it.key)
                            return@supervisorScope
                        }

                        continuation.resume(null)
                        return@supervisorScope
                    }
                }
            }
        }
    }

    private fun retrieveCommands(jda: JDA, guildId: Long): RestAction<DataArray> {
        val route = Route.Interactions.GET_GUILD_COMMANDS.compile(jda.selfUser.applicationId, guildId.toString())

        return RestActionImpl(jda, route) { res, _ -> res.array }
    }

    private fun buildCommands(guild: Guild, commandsArray: DataArray): List<Command> {
        return commandsArray.stream(DataArray::getObject)
            .map { CommandImpl(guild.jda as JDAImpl, guild, it) }
            .toList()
    }
}