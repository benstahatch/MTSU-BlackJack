package com.mtsu.table21.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.mtsu.table21.model.Card;

class ShoeTest {

    @Test
    void oneDeckShoeContains52Cards() {

        Shoe shoe = new Shoe(1);

        assertEquals(52, shoe.size());
    }


    @Test
    void sixDeckShoeContains312Cards() {

        Shoe shoe = new Shoe(6);

        assertEquals(312, shoe.size());
    }


    @Test
    void dealingCardReducesShoeSize() {

        Shoe shoe = new Shoe(2);

        Card card = shoe.dealCard();

        assertNotNull(card);
        assertEquals(103, shoe.size());
    }


    @Test
    void resetRestoresAllCards() {

        Shoe shoe = new Shoe(2);

        shoe.dealCard();
        shoe.dealCard();
        shoe.dealCard();

        assertEquals(101, shoe.size());

        shoe.reset();

        assertEquals(104, shoe.size());
    }


    @Test
    void emptyShoeReturnsNull() {

        Shoe shoe = new Shoe(1);

        for (int i = 0; i < 52; i++) {
            shoe.dealCard();
        }

        assertTrue(shoe.isEmpty());
        assertNull(shoe.dealCard());
    }


    @Test
    void zeroDecksIsNotAllowed() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new Shoe(0)
        );
    }
}
