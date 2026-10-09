package com.mtsu.table21.game;

import com.mtsu.table21.model.Hand;

public class BlackjackRules {

    // Possible results after player and dealer finish
    public enum Result {
        PLAYER_WIN,
        DEALER_WIN,
        PUSH
    }


    // Dealer hits when below 17
    public static boolean shouldDealerHit(Hand dealerHand) {

        return dealerHand.getValue() < 17;
    }


    // Player may split only with exactly two cards
    // having the same rank
    public static boolean canSplit(Hand hand) {

        if (hand.getCardCount() != 2) {
            return false;
        }

        return hand.getCard(0).getRank()
                == hand.getCard(1).getRank();
    }


    // Simplified rule:
    // double down allowed only with first two cards
    public static boolean canDoubleDown(Hand hand) {

        return hand.getCardCount() == 2;
    }


    // Determine winner after both hands are finished
    public static Result determineResult(
            Hand playerHand,
            Hand dealerHand) {

        // Player bust always loses
        if (playerHand.isBust()) {
            return Result.DEALER_WIN;
        }

        // Dealer bust means player wins
        if (dealerHand.isBust()) {
            return Result.PLAYER_WIN;
        }

        // Natural blackjack beats a normal 21
        if (playerHand.isBlackjack()
                && !dealerHand.isBlackjack()) {

            return Result.PLAYER_WIN;
        }

        if (dealerHand.isBlackjack()
                && !playerHand.isBlackjack()) {

            return Result.DEALER_WIN;
        }

        // Both have blackjack
        if (playerHand.isBlackjack()
                && dealerHand.isBlackjack()) {

            return Result.PUSH;
        }


        int playerValue = playerHand.getValue();
        int dealerValue = dealerHand.getValue();


        if (playerValue > dealerValue) {
            return Result.PLAYER_WIN;
        }

        if (dealerValue > playerValue) {
            return Result.DEALER_WIN;
        }

        return Result.PUSH;
    }
}
