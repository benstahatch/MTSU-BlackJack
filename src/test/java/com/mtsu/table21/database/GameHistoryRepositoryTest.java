
package com.mtsu.table21.database;

import com.mtsu.table21.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GameHistoryRepositoryTest {

    @Test
    void testSaveAndRetrieveGameHistory() throws Exception {

        UserRepository userRepository = new UserRepository();
        GameHistoryRepository historyRepository = new GameHistoryRepository();

        String username = "history_" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 12);

        String roundId = UUID.randomUUID().toString();
        long userId = -1;

        try {
            User user = userRepository.createUser(
                    username, "test_hash_only");

            userId = user.getId();

            historyRepository.saveGame(
                    roundId,
                    userId,
                    0,
                    "PLAYER_WIN",
                    new BigDecimal("100.00"),
                    new BigDecimal("200.00"),
                    false,
                    false
            );

            List<String> history = historyRepository.getHistory(userId);

            assertFalse(history.isEmpty());
            assertTrue(history.stream()
                    .anyMatch(entry -> entry.contains(roundId)));
            assertTrue(history.stream()
                    .anyMatch(entry -> entry.contains("PLAYER_WIN")));

            System.out.println("Game history test passed!");

        } finally {
            if (userId > 0) {
                try (Connection connection = DatabaseConnection.getConnection()) {

                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM game_history WHERE user_id = ?")) {
                        statement.setLong(1, userId);
                        statement.executeUpdate();
                    }

                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM users WHERE id = ?")) {
                        statement.setLong(1, userId);
                        statement.executeUpdate();
                    }
                }
            }
        }
    }
}
