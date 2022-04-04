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

package at.xirado.tuner.data

import net.dv8tion.jda.api.utils.data.DataObject

class GuildData(val guildId: Long, val dataObject: DataObject) {

    suspend fun update(): GuildData {
        Database.connection.use { connection ->
            connection.prepareStatement("INSERT INTO guild_data values (?,?) ON CONFLICT(guild_id) DO UPDATE SET data=?").use { ps ->
                val jsonString = dataObject.toString()

                ps.setLong(1, guildId)
                ps.setString(2, jsonString)
                ps.setString(3, jsonString)
                ps.executeAsync()
            }
        }
        return this
    }
}