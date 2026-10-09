package com.mtsu.table21.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HandTest {

    @Test
    void calculatesNormalHandValue() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.TEN));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SEVEN));

        assertEquals(17, hand.getValue());
    }


    @Test
    void aceCanBeWorthEleven() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.ACE));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SIX));

        assertEquals(17, hand.getValue());
    }


    @Test
    void aceChangesToOneToAvoidBust() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.ACE));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SIX));

        hand.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.KING));

        assertEquals(17, hand.getValue());
    }


    @Test
    void handlesMultipleAces() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.ACE));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.ACE));

        hand.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.NINE));

        assertEquals(21, hand.getValue());
    }


    @Test
    void detectsBust() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.KING));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.QUEEN));

        hand.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.TWO));

        assertTrue(hand.isBust());
    }


    @Test
    void detectsBlackjack() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.ACE));

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.KING));

        assertTrue(hand.isBlackjack());
    }


    @Test
    void twentyOneWithThreeCardsIsNotBlackjack() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.SEVEN));

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SEVEN));

        assertFalse(hand.isBlackjack());
    }
}

