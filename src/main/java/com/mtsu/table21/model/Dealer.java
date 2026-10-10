package com.mtsu.table21.model;

public class Dealer {

    // Dealer's current hand
    private Hand hand;


    // Constructor
    public Dealer() {

        hand = new Hand();
    }


    // Return dealer's hand
    public Hand getHand() {

        return hand;
    }


    // Give dealer a card
    public void addCard(Card card) {

        hand.addCard(card);
    }


    // Start a new round
    public void resetHand() {

        hand = new Hand();
    }
}
