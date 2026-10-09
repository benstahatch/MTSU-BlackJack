package com.mtsu.table21.game;

import com.mtsu.table21.model.Card;

import java.util.ArrayList;
import java.util.Collections;

public class Shoe {

    // All cards currently inside the shoe
    private ArrayList<Card> cards;

    // Number of decks used in this shoe
    private final int numberOfDecks;


    // Constructor
    public Shoe(int numberOfDecks) {

        // A shoe must contain at least one deck
        if (numberOfDecks <= 0) {
            throw new IllegalArgumentException(
                    "Number of decks must be greater than 0");
        }

        this.numberOfDecks = numberOfDecks;

        reset();
    }


    // Rebuild the shoe using the original number of decks
    public void reset() {

        cards = new ArrayList<>();


        // Create each deck
        for (int i = 0; i < numberOfDecks; i++) {

            Deck deck = new Deck();


            // Move all 52 cards from this deck into the shoe
            while (!deck.isEmpty()) {

                cards.add(deck.dealCard());
            }
        }


        // Shuffle all decks together
        shuffle();
    }


    // Shuffle all cards in the shoe
    public void shuffle() {

        Collections.shuffle(cards);
    }


    // Deal one card
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


    // Check whether shoe is empty
    public boolean isEmpty() {

        return cards.isEmpty();
    }


    // Get number of decks used
    public int getNumberOfDecks() {

        return numberOfDecks;
    }
}
