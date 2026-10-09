package com.mtsu.table21.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.mtsu.table21.model.Card;
import com.mtsu.table21.model.Hand;

class BlackjackRulesTest {


    @Test
    void dealerHitsBelow17() {

        Hand dealer = new Hand();

        dealer.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.TEN));

        dealer.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SIX));

        assertTrue(
            BlackjackRules.shouldDealerHit(dealer));
    }


    @Test
    void dealerStandsOn17() {

        Hand dealer = new Hand();

        dealer.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.TEN));

        dealer.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SEVEN));

        assertFalse(
            BlackjackRules.shouldDealerHit(dealer));
    }


    @Test
    void matchingRanksCanSplit() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.EIGHT));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.EIGHT));

        assertTrue(
            BlackjackRules.canSplit(hand));
    }


    @Test
    void differentRanksCannotSplit() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.EIGHT));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SEVEN));

        assertFalse(
            BlackjackRules.canSplit(hand));
    }


    @Test
    void twoCardsCanDoubleDown() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.FIVE));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SIX));

        assertTrue(
            BlackjackRules.canDoubleDown(hand));
    }


    @Test
    void threeCardsCannotDoubleDown() {

        Hand hand = new Hand();

        hand.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.TWO));

        hand.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.THREE));

        hand.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.FOUR));

        assertFalse(
            BlackjackRules.canDoubleDown(hand));
    }


    @Test
    void playerBustMeansDealerWins() {

        Hand player = new Hand();
        Hand dealer = new Hand();

        player.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.KING));

        player.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.QUEEN));

        player.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.TWO));

        dealer.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.TEN));

        dealer.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.SEVEN));


        assertEquals(
            BlackjackRules.Result.DEALER_WIN,
            BlackjackRules.determineResult(player, dealer));
    }


    @Test
    void dealerBustMeansPlayerWins() {

        Hand player = new Hand();
        Hand dealer = new Hand();

        player.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.TEN));

        player.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.EIGHT));

        dealer.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.KING));

        dealer.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.QUEEN));

        dealer.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.TWO));


        assertEquals(
            BlackjackRules.Result.PLAYER_WIN,
            BlackjackRules.determineResult(player, dealer));
    }


    @Test
    void higherPlayerHandWins() {

        Hand player = new Hand();
        Hand dealer = new Hand();

        player.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.TEN));

        player.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.NINE));

        dealer.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.TEN));

        dealer.addCard(
            new Card(Card.Suit.DIAMONDS, Card.Rank.EIGHT));


        assertEquals(
            BlackjackRules.Result.PLAYER_WIN,
            BlackjackRules.determineResult(player, dealer));
    }


    @Test
    void equalHandsArePush() {

        Hand player = new Hand();
        Hand dealer = new Hand();

        player.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.TEN));

        player.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.EIGHT));

        dealer.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.TEN));

        dealer.addCard(
            new Card(Card.Suit.DIAMONDS, Card.Rank.EIGHT));


        assertEquals(
            BlackjackRules.Result.PUSH,
            BlackjackRules.determineResult(player, dealer));
    }


    @Test
    void blackjackBeatsNormal21() {

        Hand player = new Hand();
        Hand dealer = new Hand();

        // Player natural blackjack
        player.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.ACE));

        player.addCard(
            new Card(Card.Suit.CLUBS, Card.Rank.KING));


        // Dealer has 21 using three cards
        dealer.addCard(
            new Card(Card.Suit.SPADES, Card.Rank.SEVEN));

        dealer.addCard(
            new Card(Card.Suit.DIAMONDS, Card.Rank.SEVEN));

        dealer.addCard(
            new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));


        assertEquals(
            BlackjackRules.Result.PLAYER_WIN,
            BlackjackRules.determineResult(player, dealer));
    }
}
