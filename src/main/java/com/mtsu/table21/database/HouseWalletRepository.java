
package com.mtsu.table21.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HouseWalletRepository {

    public BigDecimal getBalance() throws SQLException {

        String sql = "SELECT balance FROM house_wallet WHERE id = 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            if (results.next()) {
                return results.getBigDecimal("balance");
            }
        }

        throw new SQLException("House wallet not found.");
    }

    public boolean addChips(BigDecimal amount) throws SQLException {

        validateAmount(amount);

        String sql = """
                UPDATE house_wallet
                SET balance = balance + ?
                WHERE id = 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, amount);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean removeChips(BigDecimal amount) throws SQLException {

        validateAmount(amount);

        String sql = """
                UPDATE house_wallet
                SET balance = balance - ?
                WHERE id = 1 AND balance >= ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, amount);
            statement.setBigDecimal(2, amount);

            return statement.executeUpdate() == 1;
        }
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null
                || amount.signum() <= 0
                || amount.scale() > 2
                || amount.precision() - amount.scale() > 13) {

            throw new IllegalArgumentException(
                    "Invalid chip amount.");
        }
    }
}
