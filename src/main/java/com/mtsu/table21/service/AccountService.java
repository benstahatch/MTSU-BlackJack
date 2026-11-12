
package com.mtsu.table21.service;

import com.mtsu.table21.database.DatabaseConnection;
import com.mtsu.table21.database.UserRepository;
import com.mtsu.table21.model.User;
import com.mtsu.table21.model.Wallet;
import com.mtsu.table21.security.PasswordService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class AccountService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public AccountService() {
        this.userRepository = new UserRepository();
        this.passwordService = new PasswordService();
    }

    public User register(String username, String password)
            throws SQLException {

        validateUsername(username);
        validatePassword(password);

        String passwordHash = passwordService.hashPassword(password);

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                long userId;

                String userSql = """
                        INSERT INTO users (username, password_hash)
                        VALUES (?, ?)
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     userSql, Statement.RETURN_GENERATED_KEYS)) {

                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to create user.");
                        }
                        userId = keys.getLong(1);
                    }
                }

                String walletSql = """
                        INSERT INTO wallets (user_id, balance)
                        VALUES (?, ?)
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(walletSql)) {

                    statement.setLong(1, userId);
                    statement.setBigDecimal(
                            2,
                            BigDecimal.valueOf(
                                    Wallet.DEFAULT_STARTING_BALANCE));
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO leaderboard (user_id, username, peak_balance) VALUES (?, ?, 1000.00)")) {
                    statement.setLong(1, userId);
                    statement.setString(2, username);
                    statement.executeUpdate();
                }

                User user;

                String selectSql = """
                        SELECT id, username, password_hash, created_at
                        FROM users
                        WHERE id = ?
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setLong(1, userId);

                    try (ResultSet results = statement.executeQuery()) {
                        if (!results.next()) {
                            throw new SQLException(
                                    "Created user could not be retrieved.");
                        }

                        Timestamp createdAt =
                                results.getTimestamp("created_at");

                        user = new User(
                                results.getLong("id"),
                                results.getString("username"),
                                results.getString("password_hash"),
                                createdAt.toLocalDateTime()
                        );
                    }
                }

                connection.commit();
                return user;

            } catch (SQLException exception) {
                connection.rollback();

                if (exception.getErrorCode() == 1062) {
                    throw new IllegalArgumentException(
                            "Username is already taken.", exception);
                }

                throw exception;

            } catch (RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public Optional<User> login(String username, String password)
            throws SQLException {

        if (username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent()
                && passwordService.verifyPassword(
                        password, user.get().getPasswordHash())) {
            return user;
        }

        return Optional.empty();
    }

    public Optional<User> findUser(long userId)
            throws SQLException {
        return userRepository.findById(userId);
    }

    private void validateUsername(String username) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty.");
        }

        if (username.length() > 50) {
            throw new IllegalArgumentException(
                    "Username cannot exceed 50 characters.");
        }
    }

    private void validatePassword(String password) {

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }
    }
}
