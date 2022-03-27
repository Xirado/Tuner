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
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class GuildData {

    private static final Logger LOG = LoggerFactory.getLogger(GuildData.class);

    private final long guildId;
    private final DataObject json;

    public GuildData(long guildId, DataObject json) {
        this.guildId = guildId;
        this.json = json;
    }

    public void update() {
        try (var connection = Database.getConnection();
             var ps = connection.prepareStatement("INSERT INTO guild_data (guild_id, data) VALUES (?, ?) ON CONFLICT(guild_id) DO UPDATE SET data=?")) {

            ps.setLong(1, guildId);
            ps.setString(2, json.toString());
            ps.setString(3, json.toString());
            ps.execute();
        } catch (SQLException exception) {
            LOG.error("Could not update guild-data for guild {}!\n  JSON:({})", guildId, json.toString(), exception);
        }
    }

    public long getGuildId() {
        return guildId;
    }

    public DataObject getDataObject() {
        return json;
    }
}
