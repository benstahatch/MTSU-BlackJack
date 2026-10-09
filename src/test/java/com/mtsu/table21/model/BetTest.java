package com.mtsu.table21.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BetTest {

    @Test
    void betStoresAmount() {

        Bet bet = new Bet(25.00);

        assertEquals(
                25.00,
                bet.getAmount());
    }


    @Test
    void normalBetKeepsOriginalAmount() {

        Bet bet = new Bet(10.00);

        assertEquals(
                10.00,
                bet.getEffectiveAmount(false));
    }


    @Test
    void doubleDownDoublesEffectiveAmount() {

        Bet bet = new Bet(10.00);

        assertEquals(
                20.00,
                bet.getEffectiveAmount(true));
    }


    @Test
    void zeroBetIsRejected() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new Bet(0)
        );
    }


    @Test
    void negativeBetIsRejected() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new Bet(-10)
        );
    }
}
