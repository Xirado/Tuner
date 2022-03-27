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

package at.xirado.tuner.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Database {

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);

    private static final HikariDataSource source;

    static {
        File dbFile = new File("database.db");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                LOG.error("Unable to create database.db file!", e);
            }
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("SQLite");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:database.db");
        config.setConnectionTestQuery("SELECT 1");
        config.setIdleTimeout(0);
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("useUnicode", "true");
        config.setMaximumPoolSize(10);
        source = new HikariDataSource(config);
        runDefaultQueries();
    }

    public static HikariDataSource getSource() {
        return source;
    }

    public static Connection getConnection() throws SQLException {
        return source.getConnection();
    }

    private static void runDefaultQueries() {
        String[] queries = {
                "CREATE TABLE IF NOT EXISTS search_history (user_id BIGINT, searched_at BIGINT, name VARCHAR(100), value VARCHAR(100), playlist BOOL)",
                "CREATE TABLE IF NOT EXISTS guild_data (guild_id BIGINT PRIMARY KEY, data TEXT)"
        };

        try (Connection connection = getConnection()) {
            for (String query : queries) {
                try(var ps = connection.prepareStatement(query)) {
                    ps.execute();
                }
            }
        } catch (SQLException exception) {
            LOG.error("Could not run default SQL queries!", exception);
        }
    }
}
