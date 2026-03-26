package com.research.pattern;

import com.research.model.*;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

// ══════════════════════════════════════════════════════════════
//  CREATIONAL PATTERN 1: SINGLETON
//  DatabaseConnectionManager ensures single connection pool handle.
//  Applied to: DB access across the whole application.
// ══════════════════════════════════════════════════════════════

/**
 * Singleton pattern - only one DatabaseConnectionManager instance exists.
 * Thread-safe via double-checked locking.
 *
 * HOW APPLIED: Every repository that needs raw JDBC access goes through
 * this manager, ensuring connection pool resources are not wasted.
 */
@Component
public class DatabaseConnectionManager {

    private static volatile DatabaseConnectionManager instance;
    private final DataSource dataSource;

    // Private constructor - prevents direct instantiation
    private DatabaseConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Double-checked locking Singleton getInstance.
     * Spring manages the bean lifecycle, but the pattern is preserved.
     */
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


// ══════════════════════════════════════════════════════════════
//  CREATIONAL PATTERN 2: FACTORY METHOD
//  UserFactory creates the correct User subclass based on role.
//  Applied to: User registration flow.
// ══════════════════════════════════════════════════════════════

/**
 * Factory Method pattern - centralises User object creation.
 *
 * HOW APPLIED: During registration, the controller passes a role string.
 * The factory returns the correct concrete User subclass without the
 * controller needing to know the class hierarchy.
 */
@Component
class UserFactory {

    /**
     * Creates and returns the appropriate User subclass.
     * OCP: adding a new role = adding a new case here, no existing code changes.
     */
    public User createUser(String name, String email, String password,
                           User.UserRole role) {
        User user = switch (role) {
            case RESEARCHER -> new Researcher();
            case ADMIN      -> new Admin();
            case REVIEWER   -> new Reviewer();
            case VISITOR    -> new Visitor();
            case COLLABORATOR -> new Collaborator();
        };
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);   // Note: hash before calling in AuthService
        user.register();              // Sets role internally
        return user;
    }
}
