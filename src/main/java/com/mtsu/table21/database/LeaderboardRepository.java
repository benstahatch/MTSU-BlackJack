package com.mtsu.table21.database;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Arcade scores outlive their active accounts. No credentials or house data are queried. */
public class LeaderboardRepository {
    public record Entry(String username, BigDecimal peakBalance, long wins) {
        public BigDecimal peakProfit() { return peakBalance.subtract(new BigDecimal("1000.00")); }
    }
    public record HandResult(int handIndex, String result, BigDecimal wager,
                             BigDecimal payout, boolean split, boolean doubled) {}

    public List<Entry> top20() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT username, peak_balance, wins FROM leaderboard
                     ORDER BY peak_balance DESC, peak_achieved_at ASC, id ASC LIMIT 20
                     """); ResultSet rows = statement.executeQuery()) {
            List<Entry> entries = new ArrayList<>();
            while (rows.next()) entries.add(new Entry(rows.getString(1), rows.getBigDecimal(2), rows.getLong(3)));
            return entries;
        }
    }

    /** History, score and account deletion commit together AFTER authoritative wallet settlement.
     * House/player transfers remain outside this transaction; callers must halt on failure. */
    public boolean completePlayer(long userId, String roundId, List<HandResult> hands) throws SQLException {
        if (hands.isEmpty()) throw new IllegalArgumentException("Completed hands required");
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal balance;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT balance FROM wallets WHERE user_id = ? FOR UPDATE")) {
                    statement.setLong(1, userId);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) throw new SQLException("Player wallet unavailable");
                        balance = rows.getBigDecimal(1).setScale(2);
                    }
                }
                long leaderboardId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM leaderboard WHERE user_id = ? FOR UPDATE")) {
                    statement.setLong(1, userId);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) throw new SQLException("Leaderboard record unavailable");
                        leaderboardId = rows.getLong(1);
                    }
                }
                GameHistoryRepository history = new GameHistoryRepository();
                for (HandResult hand : hands) {
                    history.saveGame(connection, roundId, userId, hand.handIndex(), hand.result(),
                            hand.wager(), hand.payout(), hand.split(), hand.doubled());
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE leaderboard SET
                        peak_achieved_at = CASE WHEN ? > peak_balance THEN CURRENT_TIMESTAMP(6) ELSE peak_achieved_at END,
                        peak_balance = GREATEST(peak_balance, ?),
                        wins = (SELECT COUNT(*) FROM game_history WHERE leaderboard_id = ? AND result = 'PLAYER_WIN')
                        WHERE id = ?
                        """)) {
                    statement.setBigDecimal(1, balance);
                    statement.setBigDecimal(2, balance);
                    statement.setLong(3, leaderboardId);
                    statement.setLong(4, leaderboardId);
                    statement.executeUpdate();
                }
                boolean bankrupt = balance.signum() == 0;
                if (bankrupt) {
                    // Wallet FK deliberately stays restrictive; explicitly remove it before the user.
                    for (String sql : List.of("DELETE FROM wallets WHERE user_id = ?", "DELETE FROM users WHERE id = ?")) {
                        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                            statement.setLong(1, userId);
                            if (statement.executeUpdate() != 1) throw new SQLException("Account deletion failed");
                        }
                    }
                }
                connection.commit();
                return bankrupt;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
