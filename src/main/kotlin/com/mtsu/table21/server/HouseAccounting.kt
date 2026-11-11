package com.mtsu.table21.server

import com.mtsu.table21.database.HouseWalletRepository
import com.mtsu.table21.model.Bet
import com.mtsu.table21.model.Wallet
import com.mtsu.table21.service.BettingService
import java.math.BigDecimal
import java.math.RoundingMode

internal class HouseAccounting(private val repository: HouseWalletRepository) {
    enum class Outcome { LOSS, PUSH, WIN, BLACKJACK }
    data class Payout(val wallet: Wallet, val wager: Double, val outcome: Outcome)
    private val betting = BettingService()

    fun collect(wagers: List<Pair<Wallet, Double>>) {
        wagers.forEach { (wallet, amount) ->
            require(amount.isFinite() && amount > 0 && wallet.canAfford(amount)) { "Not enough chips" }
            BigDecimal.valueOf(amount).setScale(2, RoundingMode.UNNECESSARY)
        }
        val total = wagers.fold(BigDecimal.ZERO) { sum, wager -> sum + BigDecimal.valueOf(wager.second) }.setScale(2)
        check(repository.addChips(total)) { "House wallet unavailable" }
        wagers.forEach { (wallet, amount) -> betting.placeBet(wallet, amount) }
    }

    fun settle(payouts: List<Payout>) {
        val credits = payouts.map { payout ->
            val credit = Wallet(0.0)
            val bet = Bet(payout.wager)
            when (payout.outcome) {
                Outcome.LOSS -> Unit
                Outcome.PUSH -> betting.refundPush(credit, bet)
                Outcome.WIN -> betting.payWin(credit, bet)
                Outcome.BLACKJACK -> betting.payBlackjack(credit, bet)
            }
            payout.wallet to BigDecimal.valueOf(credit.balance).setScale(2, RoundingMode.HALF_UP)
        }
        val total = credits.fold(BigDecimal.ZERO) { sum, credit -> sum + credit.second }
        if (total.signum() > 0) {
            check(repository.removeChips(total)) { "House cannot pay; settlement pending" }
        }
        credits.filter { it.second.signum() > 0 }.forEach { (wallet, amount) -> wallet.addChips(amount.toDouble()) }
    }
}
