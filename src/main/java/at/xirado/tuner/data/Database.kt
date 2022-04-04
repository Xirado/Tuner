package at.xirado.tuner.data

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception
import java.sql.Connection
import java.sql.SQLException

class Database {

    companion object {
        val log = LoggerFactory.getLogger(Database::class.java) as Logger
        var source: HikariDataSource

        init {
            val dbFile = File("database.db")
            if (!dbFile.exists()) {
                try {
                    dbFile.createNewFile()
                } catch (ex: Exception) {
                    log.error("Could not create database file!", ex)
                }
            }

            val config = HikariDataSource()
            config.poolName = "SQLite"
            config.driverClassName = "org.sqlite.JDBC"
            config.jdbcUrl = "jdbc:sqlite:database.db"
            config.connectionTestQuery = "SELECT 1"
            config.idleTimeout = 0
            config.addDataSourceProperty("characterEncoding", "utf8")
            config.addDataSourceProperty("useUnicode", "true")
            config.maximumPoolSize = 10
            source = HikariDataSource(config)
            runDefaultQueries()
        }

        val connection: Connection
            get() = source.connection

        private fun runDefaultQueries() {
            val queries = listOf(
                "CREATE TABLE IF NOT EXISTS search_history (user_id BIGINT, searched_at BIGINT, name VARCHAR(100), value VARCHAR(100), playlist BOOL)",
                "CREATE TABLE IF NOT EXISTS guild_data (guild_id BIGINT PRIMARY KEY, data TEXT)"
            )
            try {
                connection.use { con ->
                    for (query in queries) {
                        con.prepareStatement(query).use { it.execute() }
                    }
                }
            } catch (ex: SQLException) {
                log.error("Could not run default SQL queries!", ex)
            }
        }
    }
}