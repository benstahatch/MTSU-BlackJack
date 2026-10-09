package com.mtsu.table21.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.mtsu.table21.model.Card;
import com.mtsu.table21.model.Player;

class GameRoundTest {

    /*
     * Special shoe used only for testing.
     *
     * Instead of random cards, this lets us choose
     * exactly which cards will be dealt and in what order.
     */
    private static class FixedShoe extends Shoe {

        private final ArrayList<Card> fixedCards;

        public FixedShoe(Card... cards) {

            super(1);

            fixedCards = new ArrayList<>();

            for (Card card : cards) {
                fixedCards.add(card);
            }
        }


        @Override
        public Card dealCard() {

            if (fixedCards.isEmpty()) {
                return null;
            }

            return fixedCards.remove(0);
        }


        @Override
        public int size() {
            return fixedCards.size();
        }


        @Override
        public boolean isEmpty() {
            return fixedCards.isEmpty();
        }
    }


    // Helper method to make test cards easier to create
    private Card card(Card.Suit suit, Card.Rank rank) {

        return new Card(suit, rank);
    }


    @Test
    void newRoundStartsAsNotStarted() {

        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, new Shoe(1));

        assertEquals(
                GameRound.State.NOT_STARTED,
                round.getState());

        assertNull(round.getResult());
    }


    @Test
    void startingRoundDealsTwoCardsToPlayerAndDealer() {

        FixedShoe shoe = new FixedShoe(

            // Player card 1
            card(Card.Suit.HEARTS, Card.Rank.FIVE),

            // Dealer card 1
            card(Card.Suit.CLUBS, Card.Rank.TEN),

            // Player card 2
            card(Card.Suit.SPADES, Card.Rank.SIX),

            // Dealer card 2
            card(Card.Suit.DIAMONDS, Card.Rank.SEVEN)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();


        assertEquals(
                2,
                player.getHand().getCardCount());

        assertEquals(
                2,
                round.getDealer()
                     .getHand()
                     .getCardCount());

        assertEquals(
                11,
                player.getHand().getValue());

        assertEquals(
                17,
                round.getDealer()
                     .getHand()
                     .getValue());

        assertEquals(
                GameRound.State.PLAYER_TURN,
                round.getState());
    }


    @Test
    void playerCanHit() {

        FixedShoe shoe = new FixedShoe(

            card(Card.Suit.HEARTS, Card.Rank.FIVE),
            card(Card.Suit.CLUBS, Card.Rank.TEN),

            card(Card.Suit.SPADES, Card.Rank.SIX),
            card(Card.Suit.DIAMONDS, Card.Rank.SEVEN),

            // Card received from hit
            card(Card.Suit.HEARTS, Card.Rank.TWO)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();

        Card dealtCard = round.hit();


        assertEquals(
                Card.Rank.TWO,
                dealtCard.getRank());

        assertEquals(
                3,
                player.getHand().getCardCount());

        assertEquals(
                13,
                player.getHand().getValue());

        assertEquals(
                GameRound.State.PLAYER_TURN,
                round.getState());
    }


    @Test
    void playerBustEndsRound() {

        FixedShoe shoe = new FixedShoe(

            // Player starts with 19
            card(Card.Suit.HEARTS, Card.Rank.TEN),

            // Dealer
            card(Card.Suit.CLUBS, Card.Rank.TEN),

            card(Card.Suit.SPADES, Card.Rank.NINE),

            // Dealer total = 17
            card(Card.Suit.DIAMONDS, Card.Rank.SEVEN),

            // Player hits 5 -> 24
            card(Card.Suit.HEARTS, Card.Rank.FIVE)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();

        round.hit();


        assertTrue(
                player.getHand().isBust());

        assertTrue(
                round.isFinished());

        assertEquals(
                BlackjackRules.Result.DEALER_WIN,
                round.getResult());
    }


    @Test
    void standStartsDealerTurnAndFinishesRound() {

        FixedShoe shoe = new FixedShoe(

            // Player = 18
            card(Card.Suit.HEARTS, Card.Rank.TEN),

            // Dealer starts with 16
            card(Card.Suit.CLUBS, Card.Rank.TEN),

            card(Card.Suit.SPADES, Card.Rank.EIGHT),

            card(Card.Suit.DIAMONDS, Card.Rank.SIX),

            // Dealer hits and receives 2
            // Dealer becomes 18
            card(Card.Suit.HEARTS, Card.Rank.TWO)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();

        round.stand();


        assertTrue(
                round.isFinished());

        assertEquals(
                3,
                round.getDealer()
                     .getHand()
                     .getCardCount());

        assertEquals(
                18,
                round.getDealer()
                     .getHand()
                     .getValue());

        assertEquals(
                BlackjackRules.Result.PUSH,
                round.getResult());
    }


    @Test
    void playerCanSplitMatchingCards() {

        FixedShoe shoe = new FixedShoe(

            // Player gets two Eights
            card(Card.Suit.HEARTS, Card.Rank.EIGHT),

            // Dealer card
            card(Card.Suit.CLUBS, Card.Rank.TEN),

            card(Card.Suit.SPADES, Card.Rank.EIGHT),

            // Dealer = 17
            card(Card.Suit.DIAMONDS, Card.Rank.SEVEN),

            // New card for first split hand
            card(Card.Suit.HEARTS, Card.Rank.THREE),

            // New card for second split hand
            card(Card.Suit.CLUBS, Card.Rank.FOUR)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();

        round.split();


        assertTrue(
                round.wasSplit());

        assertEquals(
                2,
                player.getHandCount());


        // First hand = 8 + 3 = 11
        assertEquals(
                11,
                player.getHand(0).getValue());


        // Second hand = 8 + 4 = 12
        assertEquals(
                12,
                player.getHand(1).getValue());


        assertEquals(
                GameRound.State.PLAYER_TURN,
                round.getState());

        assertEquals(
                0,
                round.getCurrentHandIndex());
    }


    @Test
    void splitHandsArePlayedOneAtATime() {

        FixedShoe shoe = new FixedShoe(

            card(Card.Suit.HEARTS, Card.Rank.EIGHT),

            card(Card.Suit.CLUBS, Card.Rank.TEN),

            card(Card.Suit.SPADES, Card.Rank.EIGHT),

            // Dealer = 17
            card(Card.Suit.DIAMONDS, Card.Rank.SEVEN),

            // Hand 0 becomes 11
            card(Card.Suit.HEARTS, Card.Rank.THREE),

            // Hand 1 becomes 12
            card(Card.Suit.CLUBS, Card.Rank.FOUR)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();

        round.split();


        // Stand on first hand
        round.stand();


        assertEquals(
                1,
                round.getCurrentHandIndex());

        assertEquals(
                GameRound.State.PLAYER_TURN,
                round.getState());


        // Stand on second hand
        round.stand();


        assertTrue(
                round.isFinished());

        assertEquals(
                2,
                round.getResults().size());

        assertEquals(
                BlackjackRules.Result.DEALER_WIN,
                round.getResult(0));

        assertEquals(
                BlackjackRules.Result.DEALER_WIN,
                round.getResult(1));
    }


    @Test
    void playerCanDoubleDown() {

        FixedShoe shoe = new FixedShoe(

            // Player begins with 11
            card(Card.Suit.HEARTS, Card.Rank.FIVE),

            // Dealer
            card(Card.Suit.CLUBS, Card.Rank.TEN),

            card(Card.Suit.SPADES, Card.Rank.SIX),

            // Dealer total = 17
            card(Card.Suit.DIAMONDS, Card.Rank.SEVEN),

            // Double-down card = King
            // Player becomes 21
            card(Card.Suit.HEARTS, Card.Rank.KING)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();

        Card card = round.doubleDown();


        assertEquals(
                Card.Rank.KING,
                card.getRank());

        assertEquals(
                3,
                player.getHand().getCardCount());

        assertEquals(
                21,
                player.getHand().getValue());

        assertTrue(
                round.wasDoubledDown(0));

        assertTrue(
                round.isFinished());

        assertEquals(
                BlackjackRules.Result.PLAYER_WIN,
                round.getResult());
    }


    @Test
    void cannotSplitDifferentRanks() {

        FixedShoe shoe = new FixedShoe(

            // Player = 15 but cards do not match
            card(Card.Suit.HEARTS, Card.Rank.EIGHT),

            card(Card.Suit.CLUBS, Card.Rank.TEN),

            card(Card.Suit.SPADES, Card.Rank.SEVEN),

            card(Card.Suit.DIAMONDS, Card.Rank.SEVEN)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();


        assertThrows(
            IllegalStateException.class,
            () -> round.split()
        );
    }


    @Test
    void naturalBlackjackImmediatelyEndsRound() {

        FixedShoe shoe = new FixedShoe(

            // Player Blackjack
            card(Card.Suit.HEARTS, Card.Rank.ACE),

            // Dealer
            card(Card.Suit.CLUBS, Card.Rank.TEN),

            card(Card.Suit.SPADES, Card.Rank.KING),

            // Dealer = 17
            card(Card.Suit.DIAMONDS, Card.Rank.SEVEN)
        );


        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, shoe);


        round.startRound();


        assertTrue(
                player.getHand().isBlackjack());

        assertTrue(
                round.isFinished());

        assertEquals(
                BlackjackRules.Result.PLAYER_WIN,
                round.getResult());
    }
}

