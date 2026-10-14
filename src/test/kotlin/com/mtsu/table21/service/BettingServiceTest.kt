package com.mtsu.table21.service

import com.mtsu.table21.model.Wallet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BettingServiceTest {

    private val bettingService = BettingService()

    @Test
    fun placeBetRemovesChips() {

        val wallet = Wallet(1000.00)

        bettingService.placeBet(wallet, 100.00)

        assertEquals(
            900.00,
            wallet.balance,
            0.001
        )
    }

    @Test
    fun winPaysPlayer() {

        val wallet = Wallet(1000.00)

        val bet =
            bettingService.placeBet(wallet, 100.00)

        bettingService.payWin(wallet, bet)

        assertEquals(
            1100.00,
            wallet.balance,
            0.001
        )
    }

    @Test
    fun pushReturnsBet() {

        val wallet = Wallet(1000.00)

        val bet =
            bettingService.placeBet(wallet, 100.00)

        bettingService.refundPush(wallet, bet)

        assertEquals(
            1000.00,
            wallet.balance,
            0.001
        )
    }

    @Test
    fun blackjackPaysThreeToTwo() {

        val wallet = Wallet(1000.00)

        val bet =
            bettingService.placeBet(wallet, 100.00)

        bettingService.payBlackjack(wallet, bet)

        assertEquals(
            1150.00,
            wallet.balance,
            0.001
        )
    }
}
