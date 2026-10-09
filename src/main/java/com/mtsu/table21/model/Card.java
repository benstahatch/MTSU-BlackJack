package com.mtsu.table21.model;

public class Card {

    // The four possible suits
    public enum Suit {
        HEARTS,
        DIAMONDS,
        CLUBS,
        SPADES
    }

    // The thirteen possible ranks
    public enum Rank {
        ACE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
        TEN,
        JACK,
        QUEEN,
        KING
    }

    private final Suit suit;
    private final Rank rank;

    // Constructor
    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    // Returns the suit
    public Suit getSuit() {
        return suit;
    }

    // Returns the rank
    public Rank getRank() {
        return rank;
    }

    // Returns the basic Blackjack value
    public int getValue() {

        switch (rank) {

            case ACE:
                return 11;

            case TWO:
                return 2;

            case THREE:
                return 3;

            case FOUR:
                return 4;

            case FIVE:
                return 5;

            case SIX:
                return 6;

            case SEVEN:
                return 7;

            case EIGHT:
                return 8;

            case NINE:
                return 9;

            case TEN:
            case JACK:
            case QUEEN:
            case KING:
                return 10;

            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}
