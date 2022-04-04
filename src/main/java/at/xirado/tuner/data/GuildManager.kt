package at.xirado.tuner.data

import at.xirado.tuner.Application
import net.dv8tion.jda.api.utils.data.DataObject
import net.jodah.expiringmap.ExpirationListener
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class GuildManager(val application: Application) {

    companion object {
        private val log = LoggerFactory.getLogger(GuildManager::class.java) as Logger
        private val EXPIRATION_LISTENER: ExpirationListener<Long, GuildData> =
            ExpirationListener<Long, GuildData> { id, _ -> log.debug("Unloaded expired guild-data for guild $id") }
    }

    private val guildDataMap: MutableMap<Long, GuildData> = ExpiringMap.builder()
        .expirationPolicy(ExpirationPolicy.ACCESSED)
        .expiration(5, TimeUnit.MINUTES)
        .expirationListener(EXPIRATION_LISTENER)
        .build()

    fun isGuildDataLoaded(guildId: Long) = guildDataMap.containsKey(guildId)

    suspend fun getGuildData(guildId: Long): GuildData {
        val guildData = retrieveGuildData(guildId)

        if (guildData != null)
            return guildData

        return GuildData(guildId, DataObject.empty()).update()
    }

    suspend fun retrieveGuildData(guildId: Long): GuildData? {
        if (guildDataMap.containsKey(guildId))
            return guildDataMap[guildId]!!

        Database.connection.use { con ->
            con.prepareStatement("SELECT data FROM guild_data WHERE guild_id = ?").use { ps ->
                ps.setLong(1, guildId)
                val rs = ps.executeQueryAsync()
                if (rs.next()) {
                    val json = DataObject.fromJson(rs.getString("data"))
                    val guildData = GuildData(guildId, json)

                    guildDataMap[guildId] = guildData
                    rs.close()
                    return guildData
                }
                rs.close()
                return null
            }
        }
    }
}