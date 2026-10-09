package com.mtsu.table21.game;

import com.mtsu.table21.model.Card;
import com.mtsu.table21.model.Dealer;
import com.mtsu.table21.model.Player;

public class GameRound {

    // Current stage of the round
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
    private BlackjackRules.Result result;


    // Constructor
    public GameRound(Player player, Shoe shoe) {

        this.player = player;
        this.dealer = new Dealer();
        this.shoe = shoe;

        state = State.NOT_STARTED;
        result = null;
    }


    // Start a fresh blackjack round
    public void startRound() {

        player.resetHand();
        dealer.resetHand();

        result = null;
        state = State.PLAYER_TURN;


        // Deal opening cards:
        // Player, Dealer, Player, Dealer
        player.addCard(dealCard());
        dealer.addCard(dealCard());

        player.addCard(dealCard());
        dealer.addCard(dealCard());


        // Natural blackjack can immediately end the round
        if (player.getHand().isBlackjack()
                || dealer.getHand().isBlackjack()) {

            finishRound();
        }
    }


    // Player requests another card
    public Card hit() {

        if (state != State.PLAYER_TURN) {
            throw new IllegalStateException(
                    "Player cannot hit right now");
        }

        Card card = dealCard();

        player.addCard(card);


        // Bust immediately ends the round
        if (player.getHand().isBust()) {

            finishRound();
        }

        // If player reaches exactly 21,
        // automatically move to dealer turn
        else if (player.getHand().getValue() == 21) {

            stand();
        }

        return card;
    }


    // Player stops taking cards
    public void stand() {

        if (state != State.PLAYER_TURN) {
            throw new IllegalStateException(
                    "Player cannot stand right now");
        }

        state = State.DEALER_TURN;

        playDealerTurn();
    }


    // Dealer follows BlackjackRules
    private void playDealerTurn() {

        while (BlackjackRules.shouldDealerHit(
                dealer.getHand())) {

            dealer.addCard(dealCard());
        }

        finishRound();
    }


    // Calculate final result
    private void finishRound() {

        result = BlackjackRules.determineResult(
                player.getHand(),
                dealer.getHand());

        state = State.FINISHED;
    }


    // Deal safely from shoe
    private Card dealCard() {

        // If shoe has run out, rebuild it
        if (shoe.isEmpty()) {
            shoe.reset();
        }

        return shoe.dealCard();
    }


    public Player getPlayer() {
        return player;
    }


    public Dealer getDealer() {
        return dealer;
    }


    public State getState() {
        return state;
    }


    public BlackjackRules.Result getResult() {
        return result;
    }


    public boolean isFinished() {
        return state == State.FINISHED;
    }
}
