/*
 * Copyright 2022 Marcel Korzonek and the Tuner contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.xirado.tuner.data.guild;

import at.xirado.tuner.db.Database;
import at.xirado.tuner.exception.DatabaseException;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GuildManager {

    private static final Logger LOG = LoggerFactory.getLogger(GuildManager.class);

    // Doing this because there is some weird generic shit going on. (Both key and value get downcasted to Object)
    private static final ExpirationListener<Long, GuildData> EXPIRATION_LISTENER = (key, value) -> LOG.debug("Unloaded expired guild-data for guild {},", key);

    private final Map<Long, GuildData> guildDataMap = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .expiration(5, TimeUnit.SECONDS)
            .expirationListener(EXPIRATION_LISTENER)
            .build();

    private final ShardManager shardManager;

    public GuildManager(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Nonnull
    public GuildData getGuildDataOrCreate(long guildId) {
        if (guildDataMap.containsKey(guildId))
            return guildDataMap.get(guildId);

        GuildData guildData = retrieveGuildData(guildId);

        if (guildData == null) {
            guildData = new GuildData(guildId, DataObject.empty());
            guildData.update();
            LOG.debug("Created guild-data for guild {}", guildId);
        }

        guildDataMap.put(guildId, guildData);

        return guildData;
    }

    @Nullable
    public GuildData retrieveGuildData(long guildId) {
        if (guildDataMap.containsKey(guildId))
            return guildDataMap.get(guildId);

        try (var connection = Database.getConnection();
             var ps = connection.prepareStatement("SELECT data FROM guild_data WHERE guild_id = ?")) {

            ps.setLong(1, guildId);
            var rs = ps.executeQuery();

            if (rs.next()) {
                DataObject json = DataObject.fromJson(rs.getString("data"));
                GuildData guildData = new GuildData(guildId, json);

                guildDataMap.put(guildId, guildData);
                return guildData;
            }

            rs.close();
            return null;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not get guild-data of guild " + guildId + "!", exception);
        }
    }

    public ShardManager getShardManager() {
        return shardManager;
    }
}
