package com.mtsu.table21.game;

import java.util.ArrayList;
import java.util.List;

import com.mtsu.table21.model.Card;
import com.mtsu.table21.model.Dealer;
import com.mtsu.table21.model.Hand;
import com.mtsu.table21.model.Player;

public class GameRound {

    // Current stage of the Blackjack round
    public enum State {
        NOT_STARTED,
        PLAYER_TURN,
        DEALER_TURN,
        FINISHED
    }


    private final Player player;
    private final Dealer dealer;
    private final Shoe shoe;

    private State state;

    // One result for each player hand
    private final ArrayList<BlackjackRules.Result> results;

    // Which player hand is currently being played
    private int currentHandIndex;

    // We currently allow only one split per round
    private boolean splitUsed;

    // Records whether each hand was doubled down
    private boolean[] doubledDown;


    // Constructor
    public GameRound(Player player, Shoe shoe) {

        this.player = player;
        this.dealer = new Dealer();
        this.shoe = shoe;

        this.results = new ArrayList<>();

        this.state = State.NOT_STARTED;

        this.currentHandIndex = 0;
        this.splitUsed = false;

        // Maximum of two hands because we allow one split
        this.doubledDown = new boolean[2];
    }


    // Begin a new round
    public void startRound() {

        player.resetHand();
        dealer.resetHand();

        results.clear();

        currentHandIndex = 0;
        splitUsed = false;

        doubledDown = new boolean[2];

        state = State.PLAYER_TURN;


        // Initial deal:
        // Player
        // Dealer
        // Player
        // Dealer

        player.addCard(0, dealCard());

        dealer.addCard(dealCard());

        player.addCard(0, dealCard());

        dealer.addCard(dealCard());


        // Natural Blackjack immediately ends the round
        if (player.getHand().isBlackjack()
                || dealer.getHand().isBlackjack()) {

            finishRound();
        }
    }


    // Player chooses HIT
    public Card hit() {

        if (state != State.PLAYER_TURN) {

            throw new IllegalStateException(
                    "Player cannot hit right now");
        }


        Hand hand = getCurrentHand();

        Card card = dealCard();

        hand.addCard(card);


        // Bust means this hand is finished
        if (hand.isBust()) {

            moveToNextHand();
        }

        // A hand reaching exactly 21 is finished
        else if (hand.getValue() == 21) {

            moveToNextHand();
        }


        return card;
    }


    // Player chooses STAND
    public void stand() {

        if (state != State.PLAYER_TURN) {

            throw new IllegalStateException(
                    "Player cannot stand right now");
        }


        moveToNextHand();
    }


    // Player chooses SPLIT
    public void split() {

        if (state != State.PLAYER_TURN) {

            throw new IllegalStateException(
                    "Player cannot split right now");
        }


        Hand originalHand = getCurrentHand();


        // Only one split allowed in this version
        if (splitUsed
                || !BlackjackRules.canSplit(originalHand)) {

            throw new IllegalStateException(
                    "This hand cannot be split");
        }


        // Remove second card from original hand
        Card secondCard =
                originalHand.removeCard(1);


        // Create the second hand
        Hand newHand = new Hand();

        newHand.addCard(secondCard);


        // Add second hand to player
        player.addHand(newHand);


        // Deal one new card to each split hand
        originalHand.addCard(dealCard());

        newHand.addCard(dealCard());


        splitUsed = true;
    }


    // Player chooses DOUBLE DOWN
    public Card doubleDown() {

        if (state != State.PLAYER_TURN) {

            throw new IllegalStateException(
                    "Player cannot double down right now");
        }


        Hand hand = getCurrentHand();


        if (!BlackjackRules.canDoubleDown(hand)) {

            throw new IllegalStateException(
                    "This hand cannot double down");
        }


        // Record that this hand doubled
        doubledDown[currentHandIndex] = true;


        // Double down gives exactly one additional card
        Card card = dealCard();

        hand.addCard(card);


        // Player automatically stands afterward
        moveToNextHand();


        return card;
    }


    // Move from current hand to the next hand,
    // or begin dealer turn when all hands are finished
    private void moveToNextHand() {

        // Another split hand still exists
        if (currentHandIndex + 1
                < player.getHandCount()) {

            currentHandIndex++;
        }

        else {

            state = State.DEALER_TURN;

            playDealerTurn();
        }
    }


    // Dealer follows the Blackjack rules
    private void playDealerTurn() {

        // Dealer continues taking cards
        // while rules say dealer must hit
        while (BlackjackRules.shouldDealerHit(
                dealer.getHand())) {

            dealer.addCard(dealCard());
        }


        finishRound();
    }


    // Determine the result of every player hand
    private void finishRound() {

        results.clear();


        for (Hand hand : player.getHands()) {

            BlackjackRules.Result result =
                    BlackjackRules.determineResult(
                            hand,
                            dealer.getHand());

            results.add(result);
        }


        state = State.FINISHED;
    }


    // Safely deal one card from the shoe
    private Card dealCard() {

        // If shoe runs out, rebuild it
        if (shoe.isEmpty()) {

            shoe.reset();
        }


        return shoe.dealCard();
    }


    // Return the hand currently being played
    private Hand getCurrentHand() {

        return player.getHand(currentHandIndex);
    }


    // ----------------------------
    // GETTERS
    // ----------------------------

    public Player getPlayer() {
        return player;
    }


    public Dealer getDealer() {
        return dealer;
    }


    public State getState() {
        return state;
    }


    public int getCurrentHandIndex() {
        return currentHandIndex;
    }


    public boolean isFinished() {
        return state == State.FINISHED;
    }


    public boolean wasSplit() {
        return splitUsed;
    }


    public boolean wasDoubledDown(int handIndex) {
        return doubledDown[handIndex];
    }


    // Return result for a specific hand
    public BlackjackRules.Result getResult(int handIndex) {

        if (!isFinished()) {
            return null;
        }

        return results.get(handIndex);
    }


    // Keeps single-hand code convenient
    public BlackjackRules.Result getResult() {

        if (!isFinished() || results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }


    // Return all hand results
    public List<BlackjackRules.Result> getResults() {

        return List.copyOf(results);
    }
}
