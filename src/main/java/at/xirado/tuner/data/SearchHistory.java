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

package at.xirado.tuner.data;

import at.xirado.tuner.db.Database;
import at.xirado.tuner.util.autocomplete.SearchHistoryAutocompleteChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchHistory {

    private static final Logger LOG = LoggerFactory.getLogger(SearchHistory.class);

    public static void addSearchEntry(long userId, String name, String value, boolean playlist) {
        if (isDuplicate(userId, name))
            return;

        // Doing this because Discord likes to often send the name as the value instead of the actual value.
        if (name.startsWith(SearchHistoryAutocompleteChoice.SEARCH_INDICATOR) || value.startsWith(SearchHistoryAutocompleteChoice.SEARCH_INDICATOR))
            return;

        try(var connection = Database.getConnection();
            var ps = connection.prepareStatement("INSERT INTO search_history values (?,?,?,?,?)")) {

            ps.setLong(1, userId);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, name);
            ps.setString(4, value);
            ps.setBoolean(5, playlist);
            ps.execute();
        } catch (SQLException exception) {
            LOG.error("Could not add search entry to user {}!", userId, exception);
        }
    }

    public static boolean isDuplicate(long userId, String name) {
        try (var connection = Database.getConnection();
             var ps = connection.prepareStatement("SELECT 1 FROM search_history WHERE user_id = ? AND name LIKE ?")) {

            ps.setLong(1, userId);
            ps.setString(2, name);

            var rs = ps.executeQuery();

            return rs.next();
        } catch (SQLException exception) {
            LOG.error("Could not check for duplicates! (User: {}, Search-Entry: {})", userId, name, exception);
        }
        return false;
    }

    public static List<SearchHistoryAutocompleteChoice> getSearchHistory(long userId, int limit) {
        try (var connection = Database.getConnection();
             var ps = connection.prepareStatement("SELECT * FROM search_history WHERE user_id = ? ORDER BY searched_at DESC" + (limit > 0 ? " LIMIT " + limit : ""))) {

            ps.setLong(1, userId);
            var rs = ps.executeQuery();
            List<SearchHistoryAutocompleteChoice> entries = new ArrayList<>();
            while (rs.next()) {
                long searchedOn = rs.getLong("searched_at");
                String name = rs.getString("name");
                String value = rs.getString("value");
                boolean playlist = rs.getBoolean("playlist");
                entries.add(new SearchHistoryAutocompleteChoice(name, value, searchedOn, playlist));
            }
            rs.close();
            return entries;

        } catch (SQLException exception) {
            LOG.error("Could not get search history from user {}!", userId, exception);
            return Collections.emptyList();
        }
    }
}
