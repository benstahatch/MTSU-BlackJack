package com.mtsu.table21.model;

public class Bet {

    // Original amount wagered by the player
    private final double amount;


    // Constructor
    public Bet(double amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Bet amount must be greater than 0");
        }

        this.amount = amount;
    }


    // Return the original wager amount
    public double getAmount() {
        return amount;
    }


    // Return the amount that should be at risk
    // if the player doubled down
    public double getEffectiveAmount(boolean doubledDown) {

        if (doubledDown) {
            return amount * 2;
        }

        return amount;
    }
}
