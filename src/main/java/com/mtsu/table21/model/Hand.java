package com.mtsu.table21.model;

import java.util.ArrayList;

public class Hand {

    // Stores all cards currently in this hand
    private final ArrayList<Card> cards;


    // Constructor
    public Hand() {
        cards = new ArrayList<>();
    }


    // Add a card to the hand
    public void addCard(Card card) {
        cards.add(card);
    }


    // Return a specific card
    public Card getCard(int index) {
        return cards.get(index);
    }


    // Remove and return a card
    // Used when splitting a hand
    public Card removeCard(int index) {
        return cards.remove(index);
    }


    // Calculate the Blackjack value of the hand
    public int getValue() {

        int total = 0;
        int numberOfAces = 0;

        // Add all card values
        for (Card card : cards) {

            total += card.getValue();

            // Keep track of how many Aces we have
            if (card.getRank() == Card.Rank.ACE) {
                numberOfAces++;
            }
        }


        // An Ace starts as 11.
        // If the hand would bust, change an Ace
        // from 11 to 1 by subtracting 10.
        while (total > 21 && numberOfAces > 0) {

            total -= 10;
            numberOfAces--;
        }


        return total;
    }


    // Hand busts when value is greater than 21
    public boolean isBust() {
        return getValue() > 21;
    }


    // Blackjack is exactly two cards totaling 21
    public boolean isBlackjack() {
        return cards.size() == 2 && getValue() == 21;
    }


    // Number of cards currently in the hand
    public int getCardCount() {
        return cards.size();
    }


    // Remove every card from the hand
    public void clear() {
        cards.clear();
    }
}
