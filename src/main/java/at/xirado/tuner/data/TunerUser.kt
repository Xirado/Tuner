package at.xirado.tuner.data

import at.xirado.tuner.interaction.autocomplete.SearchHistoryAutocompleteChoice
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TunerUser(val guildId: Long, val userId: Long) {

    companion object {
        private val log = LoggerFactory.getLogger(TunerUser::class.java) as Logger
    }

    suspend fun addSearchEntry(name: String, value: String, playlist: Boolean) {
        if (isDuplicateSearchEntry(name))
            return

        // Doing this because Discord likes to often send the name as the value instead of the actual value.
        if (name.startsWith(SearchHistoryAutocompleteChoice.SEARCH_INDICATOR) || value.startsWith(SearchHistoryAutocompleteChoice.SEARCH_INDICATOR))
            return

        Database.connection.use { connection ->
            connection.prepareStatement("INSERT INTO search_history values (?,?,?,?,?)").use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, System.currentTimeMillis())
                ps.setString(3, name)
                ps.setString(4, value)
                ps.setBoolean(5, playlist)
                ps.executeAsync()
            }
        }
    }

    private suspend fun isDuplicateSearchEntry(name: String): Boolean {
        Database.connection.use { connection ->
            connection.prepareStatement("SELECT 1 FROM search_history WHERE user_id = ? AND name LIKE ?").use { ps ->
                ps.setLong(1, userId)
                ps.setString(2, name)

                val rs = ps.executeQueryAsync()

                return rs.next()
            }
        }
    }

    suspend fun getSearchHistory(limit: Int = 0): List<SearchHistoryAutocompleteChoice> {
        Database.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM search_history WHERE user_id = ? ORDER BY searched_at DESC${if (limit > 0) " LIMIT $limit" else ""}").use { ps ->
                ps.setLong(1, userId)
                val rs = ps.executeQueryAsync()
                val entries = mutableListOf<SearchHistoryAutocompleteChoice>()
                while (rs.next()) {
                    val searchedOn = rs.getLong("searched_at")
                    val name = rs.getString("name")
                    val value = rs.getString("value")
                    val playlist = rs.getBoolean("playlist")
                    entries.add(SearchHistoryAutocompleteChoice(name, value, searchedOn, playlist))
                }
                rs.close()
                return entries
            }
        }
    }

    suspend fun getSearchHistory(startsWith: String, limit: Int = 0): List<SearchHistoryAutocompleteChoice> {
        Database.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM search_history WHERE user_id = ? AND name LIKE ? ORDER BY searched_at DESC${if (limit > 0) " LIMIT $limit" else ""}").use { ps ->
                ps.setLong(1, userId)
                ps.setString(2, startsWith)
                val rs = ps.executeQueryAsync()
                val entries = mutableListOf<SearchHistoryAutocompleteChoice>()
                while (rs.next()) {
                    val searchedOn = rs.getLong("searched_at")
                    val name = rs.getString("name")
                    val value = rs.getString("value")
                    val playlist = rs.getBoolean("playlist")
                    entries.add(SearchHistoryAutocompleteChoice(name, value, searchedOn, playlist))
                }
                rs.close()
                return entries
            }
        }
    }
}