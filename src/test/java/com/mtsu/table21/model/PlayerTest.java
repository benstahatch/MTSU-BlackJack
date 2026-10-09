package com.mtsu.table21.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void playerStoresName() {

        Player player = new Player("Herrick");

        assertEquals("Herrick", player.getName());
    }


    @Test
    void newPlayerStartsWithOneEmptyHand() {

        Player player = new Player("Herrick");

        assertEquals(1, player.getHandCount());
        assertEquals(0, player.getHand().getCardCount());
    }


    @Test
    void playerCanReceiveCard() {

        Player player = new Player("Herrick");

        Card card =
            new Card(Card.Suit.HEARTS, Card.Rank.KING);

        player.addCard(card);

        assertEquals(1, player.getHand().getCardCount());
        assertEquals(10, player.getHand().getValue());
    }


    @Test
    void playerCanHaveMultipleHands() {

        Player player = new Player("Herrick");

        Hand secondHand = new Hand();

        player.addHand(secondHand);

        assertEquals(2, player.getHandCount());
    }


    @Test
    void resetHandReturnsPlayerToOneEmptyHand() {

        Player player = new Player("Herrick");

        player.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.ACE));

        player.addHand(new Hand());

        assertEquals(2, player.getHandCount());

        player.resetHand();

        assertEquals(1, player.getHandCount());
        assertEquals(0, player.getHand().getCardCount());
    }
}
