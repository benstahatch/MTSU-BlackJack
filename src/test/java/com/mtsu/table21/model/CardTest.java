package com.mtsu.table21.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CardTest {

    @Test
    void numberCardReturnsItsValue() {

        Card card =
            new Card(Card.Suit.HEARTS, Card.Rank.SEVEN);

        assertEquals(7, card.getValue());
    }


    @Test
    void faceCardReturnsTen() {

        Card card =
            new Card(Card.Suit.SPADES, Card.Rank.KING);

        assertEquals(10, card.getValue());
    }


    @Test
    void aceReturnsEleven() {

        Card card =
            new Card(Card.Suit.CLUBS, Card.Rank.ACE);

        assertEquals(11, card.getValue());
    }


    @Test
    void cardStoresSuitAndRank() {

        Card card =
            new Card(Card.Suit.DIAMONDS, Card.Rank.QUEEN);

        assertEquals(Card.Suit.DIAMONDS, card.getSuit());
        assertEquals(Card.Rank.QUEEN, card.getRank());
    }
}
