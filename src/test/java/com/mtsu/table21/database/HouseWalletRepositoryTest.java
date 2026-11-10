
package com.mtsu.table21.database;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class HouseWalletRepositoryTest {

    @Test
    void testHouseWalletOperations() throws Exception {

        HouseWalletRepository repository = new HouseWalletRepository();

        BigDecimal originalBalance = repository.getBalance();

        try {
            assertNotNull(originalBalance);

            assertTrue(repository.addChips(
                    new BigDecimal("500.00")));

            assertEquals(0, originalBalance
                    .add(new BigDecimal("500.00"))
                    .compareTo(repository.getBalance()));

            assertTrue(repository.removeChips(
                    new BigDecimal("200.00")));

            assertEquals(0, originalBalance
                    .add(new BigDecimal("300.00"))
                    .compareTo(repository.getBalance()));

            assertFalse(repository.removeChips(
                    repository.getBalance().add(BigDecimal.ONE)));

            assertEquals(0, originalBalance
                    .add(new BigDecimal("300.00"))
                    .compareTo(repository.getBalance()));

            System.out.println("House wallet test passed!");

        } finally {
            BigDecimal currentBalance = repository.getBalance();
            BigDecimal difference = currentBalance.subtract(originalBalance);

            if (difference.signum() > 0) {
                repository.removeChips(difference);
            } else if (difference.signum() < 0) {
                repository.addChips(difference.negate());
            }
        }
    }
}
