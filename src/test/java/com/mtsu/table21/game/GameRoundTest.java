package com.mtsu.table21.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.mtsu.table21.model.Player;

class GameRoundTest {

    @Test
    void startingRoundDealsTwoCardsToPlayerAndDealer() {

        Player player = new Player("Herrick");
        Shoe shoe = new Shoe(1);

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

        // Four cards were dealt from a 52-card shoe
        assertEquals(48, shoe.size());
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
    void roundCanBeStarted() {

        Player player = new Player("Herrick");

        GameRound round =
                new GameRound(player, new Shoe(1));

        round.startRound();

        // Normally PLAYER_TURN.
        // A random natural blackjack may immediately
        // finish the round, so either state is valid.
        assertTrue(
            round.getState() == GameRound.State.PLAYER_TURN
            ||
            round.getState() == GameRound.State.FINISHED
        );
    }
}
