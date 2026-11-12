package com.mtsu.table21.database;

import com.mtsu.table21.service.AccountService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LeaderboardRepositoryTest {
    private final LeaderboardRepository scores = new LeaderboardRepository();
    private final WalletRepository wallets = new WalletRepository();
    private static String name() { return "arcade_" + UUID.randomUUID().toString().substring(0, 12); }
    private static void update(String sql, Object... values) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
            s.executeUpdate();
        }
    }
    private static Object scalar(String sql, Object... values) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
            try (ResultSet r = s.executeQuery()) { assertTrue(r.next()); return r.getObject(1); }
        }
    }
    private static long count(String sql, Object... values) throws SQLException { return ((Number) scalar(sql, values)).longValue(); }
    private static List<LeaderboardRepository.HandResult> hand(String result, String payout) {
        return List.of(new LeaderboardRepository.HandResult(0, result, new BigDecimal("10.00"), new BigDecimal(payout), false, false));
    }
    private static void cleanup(long userId, String username) throws SQLException {
        update("DELETE FROM game_history WHERE user_id = ? OR leaderboard_id IN (SELECT id FROM leaderboard WHERE username = ?)", userId, username);
        update("DELETE FROM leaderboard WHERE username = ?", username);
        update("DELETE FROM wallets WHERE user_id = ?", userId);
        update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test void peakWinsAndHistorySurviveBankruptcyAndUsernameReuse() throws Exception {
        String username = name();
        AccountService accounts = new AccountService();
        long id = accounts.register(username, "TestPassword123!").getId();
        long replacement = -1;
        try {
            assertEquals(new BigDecimal("1000.00"), scalar("SELECT peak_balance FROM leaderboard WHERE user_id = ?", id));
            wallets.setBalance(id, new BigDecimal("1720.00"));
            String round = UUID.randomUUID().toString();
            assertFalse(scores.completePlayer(id, round, hand("PLAYER_WIN", "20.00")));
            Object achieved = scalar("SELECT peak_achieved_at FROM leaderboard WHERE user_id = ?", id);
            // Identical retries must not duplicate the hand or its win.
            scores.completePlayer(id, round, hand("PLAYER_WIN", "20.00"));
            assertEquals(1, count("SELECT COUNT(*) FROM game_history WHERE user_id = ?", id));
            assertEquals(1, count("SELECT wins FROM leaderboard WHERE user_id = ?", id));
            wallets.setBalance(id, new BigDecimal("300.00"));
            scores.completePlayer(id, UUID.randomUUID().toString(), hand("DEALER_WIN", "0.00"));
            assertEquals(new BigDecimal("1720.00"), scalar("SELECT peak_balance FROM leaderboard WHERE user_id = ?", id));
            assertEquals(achieved, scalar("SELECT peak_achieved_at FROM leaderboard WHERE user_id = ?", id));
            wallets.setBalance(id, new BigDecimal("1900.00"));
            scores.completePlayer(id, UUID.randomUUID().toString(), hand("PLAYER_WIN", "25.00"));
            scores.completePlayer(id, UUID.randomUUID().toString(), hand("PUSH", "10.00"));
            wallets.setBalance(id, BigDecimal.ZERO);
            assertTrue(scores.completePlayer(id, UUID.randomUUID().toString(), hand("DEALER_WIN", "0.00")));
            assertEquals(0, count("SELECT COUNT(*) FROM users WHERE id = ?", id));
            assertEquals(0, count("SELECT COUNT(*) FROM wallets WHERE user_id = ?", id));
            assertNull(scalar("SELECT user_id FROM leaderboard WHERE username = ?", username));
            assertEquals(new BigDecimal("1900.00"), scalar("SELECT peak_balance FROM leaderboard WHERE username = ?", username));
            assertEquals(2, count("SELECT wins FROM leaderboard WHERE username = ?", username));
            assertEquals(5, count("SELECT COUNT(*) FROM game_history h JOIN leaderboard l ON h.leaderboard_id=l.id WHERE l.username=? AND h.user_id IS NULL", username));
            assertTrue(accounts.login(username, "TestPassword123!").isEmpty());
            replacement = accounts.register(username, "NewPassword123!").getId();
            assertNotEquals(id, replacement);
            assertEquals(new BigDecimal("1000.00"), wallets.getBalance(replacement));
            assertEquals(2, count("SELECT COUNT(*) FROM leaderboard WHERE username = ?", username));
        } finally { cleanup(id, username); if (replacement > 0) cleanup(replacement, username); }
    }

    @Test void failureRollsBackHistoryAndDoesNotDeleteAccount() throws Exception {
        String username = name();
        long id = new AccountService().register(username, "TestPassword123!").getId();
        try {
            wallets.setBalance(id, BigDecimal.ZERO);
            var valid = hand("PLAYER_WIN", "20.00").getFirst();
            var invalid = new LeaderboardRepository.HandResult(1, "INVALID", new BigDecimal("10.00"), BigDecimal.ZERO, true, false);
            assertThrows(IllegalArgumentException.class, () -> scores.completePlayer(id, UUID.randomUUID().toString(), List.of(valid, invalid)));
            assertEquals(0, count("SELECT COUNT(*) FROM game_history WHERE user_id = ?", id));
            assertEquals(1, count("SELECT COUNT(*) FROM users WHERE id = ?", id));
            assertEquals(1, count("SELECT COUNT(*) FROM wallets WHERE user_id = ?", id));
            assertEquals(0, count("SELECT wins FROM leaderboard WHERE user_id = ?", id));
        } finally { cleanup(id, username); }
    }

    @Test void top20SortsByPeakThenTimeThenIdWithoutDeletingRank21() throws Exception {
        String prefix = name();
        BigDecimal base = ((BigDecimal) scalar("SELECT COALESCE(MAX(peak_balance), 1000.00) FROM leaderboard")).add(new BigDecimal("1000.00"));
        List<String> inserted = new ArrayList<>();
        try {
            for (int i = 0; i < 22; i++) {
                String username = prefix + "_" + i;
                inserted.add(username);
                // 0,1 tie on score/time; 2 has same score but later timestamp; others descend.
                BigDecimal peak = base.subtract(BigDecimal.valueOf(Math.max(0, i - 2)));
                update("INSERT INTO leaderboard (username, peak_balance, peak_achieved_at) VALUES (?, ?, ?)",
                        username, peak, i == 2 ? "2020-01-02 00:00:00" : "2020-01-01 00:00:00");
            }
            var top = scores.top20();
            assertEquals(20, top.size());
            assertEquals(inserted.subList(0, 20), top.stream().map(LeaderboardRepository.Entry::username).toList());
            assertEquals(base.subtract(new BigDecimal("1000.00")), top.getFirst().peakProfit());
            assertEquals(1, count("SELECT COUNT(*) FROM leaderboard WHERE username = ?", inserted.get(20)));
            update("UPDATE leaderboard SET peak_balance = ? WHERE username = ?", base.add(BigDecimal.ONE), inserted.get(20));
            assertEquals(inserted.get(20), scores.top20().getFirst().username());
        } finally { for (String username : inserted) update("DELETE FROM leaderboard WHERE username = ?", username); }
    }

    @Test void directUserDeletionRetainsLeaderboardAndLinkedHistory() throws Exception {
        String username = name();
        long id = new AccountService().register(username, "TestPassword123!").getId();
        try {
            scores.completePlayer(id, UUID.randomUUID().toString(), hand("PLAYER_WIN", "20.00"));
            update("DELETE FROM wallets WHERE user_id = ?", id);
            update("DELETE FROM users WHERE id = ?", id);
            assertNull(scalar("SELECT user_id FROM leaderboard WHERE username = ?", username));
            assertEquals(1, count("SELECT wins FROM leaderboard WHERE username = ?", username));
            assertEquals(1, count("SELECT COUNT(*) FROM game_history h JOIN leaderboard l ON l.id=h.leaderboard_id WHERE l.username=? AND h.user_id IS NULL", username));
        } finally { cleanup(id, username); }
    }
}
