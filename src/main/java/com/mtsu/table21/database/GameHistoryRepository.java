package com.mtsu.table21.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GameHistoryRepository {

    public void saveGame(
            String roundId,
            long userId,
            int handIndex,
            String result,
            BigDecimal betAmount,
            BigDecimal payoutAmount,
            boolean wasSplit,
            boolean wasDoubledDown
    ) throws SQLException {

        if (roundId == null || roundId.isBlank() || roundId.length() > 36) {
            throw new IllegalArgumentException("Invalid round ID");
        }

        if (userId <= 0 || handIndex < 0) {
            throw new IllegalArgumentException("Invalid user ID or hand index");
        }

        if (!"PLAYER_WIN".equals(result)
                && !"DEALER_WIN".equals(result)
                && !"PUSH".equals(result)) {
            throw new IllegalArgumentException("Invalid game result");
        }

        validateAmount(betAmount, true);
        validateAmount(payoutAmount, false);

        String sql = """
                INSERT INTO game_history
                (round_id, user_id, hand_index, result, bet_amount,
                 payout_amount, was_split, was_doubled_down)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, roundId);
            statement.setLong(2, userId);
            statement.setInt(3, handIndex);
            statement.setString(4, result);
            statement.setBigDecimal(5, betAmount);
            statement.setBigDecimal(6, payoutAmount);
            statement.setBoolean(7, wasSplit);
            statement.setBoolean(8, wasDoubledDown);

            statement.executeUpdate();
        }
    }

    public List<String> getHistory(long userId) throws SQLException {

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        String sql = """
                SELECT round_id, hand_index, result, bet_amount,
                       payout_amount, was_split, was_doubled_down, played_at
                FROM game_history
                WHERE user_id = ?
                ORDER BY played_at DESC, id DESC
                """;

        List<String> history = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String entry =
                            "Round: " + rs.getString("round_id")
                            + " | Hand: " + rs.getInt("hand_index")
                            + " | Result: " + rs.getString("result")
                            + " | Bet: " + rs.getBigDecimal("bet_amount")
                            + " | Payout: " + rs.getBigDecimal("payout_amount")
                            + " | Split: " + rs.getBoolean("was_split")
                            + " | Doubled Down: " + rs.getBoolean("was_doubled_down")
                            + " | Played: " + rs.getTimestamp("played_at");

                    history.add(entry);
                }
            }
        }

        return history;
    }

    private void validateAmount(BigDecimal amount, boolean mustBePositive) {
        if (amount == null
                || (mustBePositive && amount.signum() <= 0)
                || (!mustBePositive && amount.signum() < 0)
                || amount.scale() > 2
                || amount.precision() - amount.scale() > 13) {
            throw new IllegalArgumentException("Invalid monetary amount");
        }
    }
}
