package com.mtsu.table21.game;

import com.mtsu.table21.model.Card;

import java.util.ArrayList;
import java.util.Collections;

public class Deck {

    // Stores all cards currently remaining in the deck
    private ArrayList<Card> cards;

    // Constructor
    public Deck() {
        reset();
    }

    // Rebuild a complete 52-card deck
    public void reset() {

        cards = new ArrayList<>();

        // Go through every suit
        for (Card.Suit suit : Card.Suit.values()) {

            // Go through every rank
            for (Card.Rank rank : Card.Rank.values()) {

                // Create every possible suit/rank combination
                cards.add(new Card(suit, rank));
            }
        }

        shuffle();
    }

    // Randomize card order
    public void shuffle() {
        Collections.shuffle(cards);
    }

    // Remove and return one card from the deck
    public Card dealCard() {

        if (cards.isEmpty()) {
            return null;
        }

        return cards.remove(cards.size() - 1);
    }

    // Number of cards remaining
    public int size() {
        return cards.size();
    }

    // Check whether deck is empty
    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
