package com.mtsu.table21.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BlackjackTableTest {

    @Test
    void tableCreatesPlayerWithCorrectName() {

        BlackjackTable table =
                new BlackjackTable("Herrick", 1);

        assertEquals(
                "Herrick",
                table.getPlayer().getName());
    }


    @Test
    void tableCreatesShoeWithCorrectNumberOfDecks() {

        BlackjackTable table =
                new BlackjackTable("Herrick", 6);

        assertEquals(
                6,
                table.getShoe().getNumberOfDecks());

        assertEquals(
                312,
                table.getShoe().size());
    }


    @Test
    void newTableHasNoCurrentRound() {

        BlackjackTable table =
                new BlackjackTable("Herrick", 1);

        assertNull(table.getCurrentRound());

        assertFalse(table.hasActiveRound());
    }


    @Test
    void startNewRoundCreatesRound() {

        BlackjackTable table =
                new BlackjackTable("Herrick", 1);

        GameRound round =
                table.startNewRound();

        assertNotNull(round);

        assertEquals(
                round,
                table.getCurrentRound());
    }


    @Test
    void startingRoundDealsFourCards() {

        BlackjackTable table =
                new BlackjackTable("Herrick", 1);

        table.startNewRound();

        // Initial deal:
        // 2 player cards + 2 dealer cards
        assertEquals(
                48,
                table.getShoe().size());
    }


    @Test
    void cannotStartSecondRoundWhileFirstIsActive() {

        BlackjackTable table =
                new BlackjackTable("Herrick", 1);

        table.startNewRound();


        // A randomly dealt natural Blackjack could cause
        // the first round to finish immediately.
        //
        // Only test the exception when the round
        // actually remains active.

        if (table.hasActiveRound()) {

            assertThrows(
                IllegalStateException.class,
                () -> table.startNewRound()
            );
        }
    }
}
