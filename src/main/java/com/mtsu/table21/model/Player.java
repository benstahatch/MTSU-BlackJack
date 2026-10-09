package com.mtsu.table21.model;

import java.util.ArrayList;
import java.util.List;

public class Player {

    // Player name
    private final String name;

    // Normally contains one hand.
    // After a split, it can contain two hands.
    private final ArrayList<Hand> hands;


    // Constructor
    public Player(String name) {

        this.name = name;

        hands = new ArrayList<>();

        // Every player begins with one hand
        hands.add(new Hand());
    }


    // Return player name
    public String getName() {
        return name;
    }


    // Return first/default hand
    public Hand getHand() {
        return hands.get(0);
    }


    // Return a specific hand
    public Hand getHand(int index) {
        return hands.get(index);
    }


    // Return all player hands
    public List<Hand> getHands() {
        return hands;
    }


    // Number of hands the player currently has
    public int getHandCount() {
        return hands.size();
    }


    // Add card to first hand
    public void addCard(Card card) {
        hands.get(0).addCard(card);
    }


    // Add card to a specific hand
    public void addCard(int handIndex, Card card) {
        hands.get(handIndex).addCard(card);
    }


    // Add another hand after splitting
    public void addHand(Hand hand) {
        hands.add(hand);
    }


    // Reset player for a new round
    public void resetHand() {

        hands.clear();

        // Begin new round with one empty hand
        hands.add(new Hand());
    }
}
