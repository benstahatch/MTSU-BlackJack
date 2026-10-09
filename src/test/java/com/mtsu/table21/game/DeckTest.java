package com.mtsu.table21.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mtsu.table21.model.Card;

class DeckTest {

    @Test
    void newDeckContains52Cards() {

        Deck deck = new Deck();

        assertEquals(52, deck.size());
    }


    @Test
    void dealingCardReducesDeckSize() {

        Deck deck = new Deck();

        Card card = deck.dealCard();

        assertNotNull(card);
        assertEquals(51, deck.size());
    }


    @Test
    void deckContains52UniqueCards() {

        Deck deck = new Deck();

        Set<String> cardsSeen = new HashSet<>();

        while (!deck.isEmpty()) {

            Card card = deck.dealCard();

            String cardName =
                    card.getRank() + "-" + card.getSuit();

            cardsSeen.add(cardName);
        }

        assertEquals(52, cardsSeen.size());
    }


    @Test
    void emptyDeckReturnsNull() {

        Deck deck = new Deck();

        // Deal all 52 cards
        for (int i = 0; i < 52; i++) {
            deck.dealCard();
        }

        assertTrue(deck.isEmpty());

        // No cards remain
        assertNull(deck.dealCard());
    }


    @Test
    void resetRestoresFullDeck() {

        Deck deck = new Deck();

        deck.dealCard();
        deck.dealCard();

        assertEquals(50, deck.size());

        deck.reset();

        assertEquals(52, deck.size());
    }
}
