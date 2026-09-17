package com.mtsu.table21.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CardTest {

    @Test
    void cardReturnsValue() {
        Card card = new Card();

        assertEquals(10, card.getValue());
    }
}
