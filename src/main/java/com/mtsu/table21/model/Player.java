package com.mtsu.table21.model;

public class Player {

    // Player's name
    private final String name;

    // Player's current blackjack hand
    private Hand hand;


    // Constructor
    public Player(String name) {

        this.name = name;
        this.hand = new Hand();
    }


    // Return player's name
    public String getName() {

        return name;
    }


    // Return player's current hand
    public Hand getHand() {

        return hand;
    }


    // Add a card to player's hand
    public void addCard(Card card) {

        hand.addCard(card);
    }


    // Start a new round with an empty hand
    public void resetHand() {

        hand = new Hand();
    }
}
