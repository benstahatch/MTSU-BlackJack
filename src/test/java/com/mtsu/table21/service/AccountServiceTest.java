
package com.mtsu.table21.service;

import com.mtsu.table21.database.DatabaseConnection;
import com.mtsu.table21.database.WalletRepository;
import com.mtsu.table21.model.User;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AccountServiceTest {

    @Test
    void testRegistrationAndLogin() throws Exception {

        AccountService service = new AccountService();
        WalletRepository walletRepository = new WalletRepository();

        String username = "test_" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 12);

        String password = "TestPassword123!";
        long userId = -1;

        try {
            User registered = service.register(username, password);
            userId = registered.getId();

            assertTrue(userId > 0);
            assertEquals(username, registered.getUsername());
            assertNotEquals(password, registered.getPasswordHash());
            assertTrue(
                    registered.getPasswordHash().startsWith("pbkdf2_sha256$")
            );

            assertTrue(walletRepository.findByUserId(userId).isPresent());

            assertEquals(
                    0,
                    new BigDecimal("1000.00")
                            .compareTo(walletRepository.getBalance(userId))
            );

            User loggedIn = service.login(username, password)
                    .orElseThrow();

            assertEquals(userId, loggedIn.getId());

            assertTrue(
                    service.login(username, "WrongPassword").isEmpty()
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.register(username, password)
            );

            assertTrue(service.findUser(userId).isPresent());

            System.out.println(
                    "Registration, login, and automatic wallet test passed!"
            );

        } finally {

            if (userId > 0) {

                try (Connection connection =
                             DatabaseConnection.getConnection()) {

                    connection.setAutoCommit(false);

                    try {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "DELETE FROM leaderboard WHERE user_id = ?")) {
                            statement.setLong(1, userId);
                            statement.executeUpdate();
                        }
                        try (PreparedStatement statement =
                                     connection.prepareStatement(
                                             "DELETE FROM wallets WHERE user_id = ?")) {

                            statement.setLong(1, userId);
                            statement.executeUpdate();
                        }

                        try (PreparedStatement statement =
                                     connection.prepareStatement(
                                             "DELETE FROM users WHERE id = ?")) {

                            statement.setLong(1, userId);
                            statement.executeUpdate();
                        }

                        connection.commit();

                    } catch (Exception exception) {
                        connection.rollback();
                        throw exception;
                    }
                }
            }
        }
    }
}
