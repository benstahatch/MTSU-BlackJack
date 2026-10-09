package com.mtsu.table21.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DealerTest {

    @Test
    void newDealerStartsWithEmptyHand() {

        Dealer dealer = new Dealer();

        assertEquals(0, dealer.getHand().getCardCount());
    }


    @Test
    void dealerCanReceiveCard() {

        Dealer dealer = new Dealer();

        dealer.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SEVEN));

        dealer.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.KING));

        assertEquals(2, dealer.getHand().getCardCount());
        assertEquals(17, dealer.getHand().getValue());
    }


    @Test
    void resetHandCreatesEmptyHand() {

        Dealer dealer = new Dealer();

        dealer.addCard(
            new Card(Card.Suit.DIAMONDS, Card.Rank.QUEEN));

        dealer.resetHand();

        assertEquals(0, dealer.getHand().getCardCount());
    }
}
