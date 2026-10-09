package com.mtsu.table21.model;

import java.util.ArrayList;

public class Hand {

    // Stores the cards currently in the hand
    private ArrayList<Card> cards;

    // Constructor
    public Hand() {
        cards = new ArrayList<>();
    }

    // Add a card to the hand
    public void addCard(Card card) {
        cards.add(card);
    }

    // Calculate Blackjack value of the hand
    public int getValue() {

        int total = 0;
        int numberOfAces = 0;

        // Add the starting value of every card
        for (Card card : cards) {

            total += card.getValue();

            if (card.getRank() == Card.Rank.ACE) {
                numberOfAces++;
            }
        }

        // Change Aces from 11 to 1 when necessary
        while (total > 21 && numberOfAces > 0) {

            total -= 10;
            numberOfAces--;
        }

        return total;
    }

    // True if hand is over 21
    public boolean isBust() {
        return getValue() > 21;
    }

    // Blackjack = exactly 2 cards totaling 21
    public boolean isBlackjack() {
        return cards.size() == 2 && getValue() == 21;
    }

    // Number of cards in hand
    public int getCardCount() {
        return cards.size();
    }

    // Remove all cards
    public void clear() {
        cards.clear();
    }
}

