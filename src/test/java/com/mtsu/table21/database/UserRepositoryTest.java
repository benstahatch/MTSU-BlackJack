
package com.mtsu.table21.database;

import com.mtsu.table21.model.User;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {

    @Test
    void testCreateAndFindUser() throws Exception {

        UserRepository repository = new UserRepository();
        String username = "test_" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 12);
        String passwordHash = "test_hash_only";
        long userId = -1;

        try {
            User createdUser = repository.createUser(username, passwordHash);
            userId = createdUser.getId();

            assertTrue(userId > 0);
            assertEquals(username, createdUser.getUsername());
            assertEquals(passwordHash, createdUser.getPasswordHash());
            assertNotNull(createdUser.getCreatedAt());

            User foundByUsername = repository.findByUsername(username)
                    .orElseThrow();

            assertEquals(userId, foundByUsername.getId());

            User foundById = repository.findById(userId)
                    .orElseThrow();

            assertEquals(username, foundById.getUsername());

            System.out.println("User repository test passed!");

        } finally {
            if (userId > 0) {
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "DELETE FROM users WHERE id = ?")) {

                    statement.setLong(1, userId);
                    statement.executeUpdate();
                }
            }
        }
    }
}
