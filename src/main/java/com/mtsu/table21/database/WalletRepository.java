
package com.mtsu.table21.database;

import com.mtsu.table21.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class WalletRepository {

    public void createWallet(long userId) throws SQLException {
        String sql = """
                INSERT INTO wallets (user_id, balance)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);
            statement.setBigDecimal(
                    2,
                    BigDecimal.valueOf(Wallet.DEFAULT_STARTING_BALANCE)
            );

            statement.executeUpdate();
        }
    }

    public Optional<Wallet> findByUserId(long userId) throws SQLException {
        String sql = """
                SELECT balance
                FROM wallets
                WHERE user_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    BigDecimal balance = results.getBigDecimal("balance");
                    return Optional.of(new Wallet(balance.doubleValue()));
                }
            }
        }

        return Optional.empty();
    }

    public BigDecimal getBalance(long userId) throws SQLException {
        String sql = """
                SELECT balance
                FROM wallets
                WHERE user_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getBigDecimal("balance");
                }
            }
        }

        throw new SQLException("Wallet not found for user: " + userId);
    }

    public boolean addChips(long userId, BigDecimal amount)
            throws SQLException {

        validateAmount(amount);

        String sql = """
                UPDATE wallets
                SET balance = balance + ?
                WHERE user_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, amount);
            statement.setLong(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean removeChips(long userId, BigDecimal amount)
            throws SQLException {

        validateAmount(amount);

        String sql = """
                UPDATE wallets
                SET balance = balance - ?
                WHERE user_id = ?
                AND balance >= ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, amount);
            statement.setLong(2, userId);
            statement.setBigDecimal(3, amount);

            return statement.executeUpdate() == 1;
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0
                || amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Chip amount must be positive with at most 2 decimal places.");
        }
    }
}
