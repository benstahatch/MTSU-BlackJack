
package com.mtsu.table21.database;

import com.mtsu.table21.model.User;
import com.mtsu.table21.model.Wallet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WalletRepositoryTest {

    @Test
    void rejectsInvalidSavedBalances() {
        WalletRepository repository = new WalletRepository();
        assertThrows(IllegalArgumentException.class,
                () -> repository.setBalance(1, new BigDecimal("-1.00")));
        assertThrows(ArithmeticException.class,
                () -> repository.setBalance(1, new BigDecimal("1.001")));
    }

    @Test
    void testWalletOperations() throws Exception {

        UserRepository userRepository = new UserRepository();
        WalletRepository walletRepository = new WalletRepository();

        String username = "wallet_" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 12);

        long userId = -1;

        try {
            User user = userRepository.createUser(username, "test_hash_only");
            userId = user.getId();

            walletRepository.createWallet(userId);

            assertEquals(
                    0,
                    BigDecimal.valueOf(Wallet.DEFAULT_STARTING_BALANCE)
                            .compareTo(walletRepository.getBalance(userId))
            );

            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow();

            assertEquals(1000.00, wallet.getBalance(), 0.001);

            assertTrue(walletRepository.addChips(
                    userId, new BigDecimal("500.00")));

            assertEquals(
                    0,
                    new BigDecimal("1500.00")
                            .compareTo(walletRepository.getBalance(userId))
            );

            assertTrue(walletRepository.removeChips(
                    userId, new BigDecimal("200.00")));

            assertEquals(
                    0,
                    new BigDecimal("1300.00")
                            .compareTo(walletRepository.getBalance(userId))
            );

            assertFalse(walletRepository.removeChips(
                    userId, new BigDecimal("2000.00")));

            assertEquals(
                    0,
                    new BigDecimal("1300.00")
                            .compareTo(walletRepository.getBalance(userId))
            );

            walletRepository.setBalance(userId, new BigDecimal("1010.00"));
            assertEquals(new BigDecimal("1010.00"), walletRepository.getBalance(userId));
            walletRepository.setBalance(userId, BigDecimal.ZERO);
            assertEquals(new BigDecimal("0.00"), walletRepository.getBalance(userId));
            assertTrue(userRepository.findById(userId).isPresent());

        } finally {
            if (userId > 0) {
                try (Connection connection = DatabaseConnection.getConnection()) {

                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM wallets WHERE user_id = ?")) {
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
