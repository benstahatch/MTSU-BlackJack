package com.mtsu.table21.service

import com.mtsu.table21.model.Bet
import com.mtsu.table21.model.Wallet

class BettingService {

    // Create a bet and remove those chips from the wallet
    fun placeBet(wallet: Wallet, amount: Double): Bet {

        val bet = Bet(amount)

        wallet.removeChips(amount)

        return bet
    }


    // Normal win:
    // return original bet + equal winnings
    fun payWin(wallet: Wallet, bet: Bet) {

        wallet.addChips(bet.amount * 2)
    }


    // Push:
    // player simply gets original wager back
    fun refundPush(wallet: Wallet, bet: Bet) {

        wallet.addChips(bet.amount)
    }


    // Blackjack pays 3:2
    // 100 bet = 150 profit + original 100 returned
    fun payBlackjack(wallet: Wallet, bet: Bet) {

        wallet.addChips(bet.amount * 2.5)
    }
}
