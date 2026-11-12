
package com.mtsu.table21.database;

import com.mtsu.table21.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class UserRepository {

    public User createUser(String username, String passwordHash)
            throws SQLException {

        String sql = """
                INSERT INTO users (username, password_hash)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long userId = keys.getLong(1);
                    return findById(userId)
                            .orElseThrow(() -> new SQLException(
                                    "Created user could not be retrieved."));
                }
            }

            throw new SQLException("Failed to retrieve generated user ID.");
        }
    }

    public Optional<User> findByUsername(String username)
            throws SQLException {

        String sql = """
                SELECT id, username, password_hash, created_at
                FROM users
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return Optional.of(mapUser(results));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<User> findById(long userId)
            throws SQLException {

        String sql = """
                SELECT id, username, password_hash, created_at
                FROM users
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return Optional.of(mapUser(results));
                }
            }
        }

        return Optional.empty();
    }

    private User mapUser(ResultSet results) throws SQLException {
        Timestamp createdAt = results.getTimestamp("created_at");

        return new User(
                results.getLong("id"),
                results.getString("username"),
                results.getString("password_hash"),
                createdAt.toLocalDateTime()
        );
    }
}
