package com.mtsu.table21.game;

import com.mtsu.table21.model.Player;

public class BlackjackTable {

    // Player using this table
    private final Player player;

    // Shoe stays alive across multiple rounds
    private final Shoe shoe;

    // Current round being played
    private GameRound currentRound;


    // Constructor
    public BlackjackTable(String playerName, int numberOfDecks) {

        player = new Player(playerName);

        shoe = new Shoe(numberOfDecks);

        currentRound = null;
    }


    // Start a new Blackjack round
    public GameRound startNewRound() {

        // Do not start another round while
        // the current round is still active
        if (currentRound != null
                && !currentRound.isFinished()) {

            throw new IllegalStateException(
                    "A round is already in progress");
        }


        currentRound =
                new GameRound(player, shoe);

        currentRound.startRound();

        return currentRound;
    }


    // Return player at this table
    public Player getPlayer() {
        return player;
    }


    // Return the shoe
    public Shoe getShoe() {
        return shoe;
    }


    // Return current round
    public GameRound getCurrentRound() {
        return currentRound;
    }


    // True if a round currently exists
    // and has not finished
    public boolean hasActiveRound() {

        return currentRound != null
                && !currentRound.isFinished();
    }
}
