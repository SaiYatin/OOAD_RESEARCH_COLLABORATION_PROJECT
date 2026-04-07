package com.research.pattern;

import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton pattern - only one DatabaseConnectionManager instance exists.
 */
@Component
public class DatabaseConnectionManager {

    private static volatile DatabaseConnectionManager instance;
    private final DataSource dataSource;

    private DatabaseConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static DatabaseConnectionManager getInstance(DataSource dataSource) {
        if (instance == null) {
            synchronized (DatabaseConnectionManager.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionManager(dataSource);
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
