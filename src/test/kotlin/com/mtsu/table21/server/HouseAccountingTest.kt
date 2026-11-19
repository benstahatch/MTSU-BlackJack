package com.mtsu.table21.server

import com.mtsu.table21.database.HouseWalletRepository
import com.mtsu.table21.model.Wallet
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class HouseAccountingTest {
    private class House : HouseWalletRepository() {
        var chips = BigDecimal("100000.00")
        var rejectDeposits = false
        override fun getBalance() = chips
        override fun addChips(amount: BigDecimal): Boolean {
            if (rejectDeposits) return false
            chips += amount
            return true
        }
        override fun removeChips(amount: BigDecimal): Boolean {
            if (chips < amount) return false
            chips -= amount
            return true
        }
    }

    @Test
    fun wagersAndAllOutcomesConserveMoney() {
        val expected = mapOf(
            HouseAccounting.Outcome.LOSS to 990.0,
            HouseAccounting.Outcome.PUSH to 1000.0,
            HouseAccounting.Outcome.WIN to 1010.0,
            HouseAccounting.Outcome.BLACKJACK to 1015.0
        )
        expected.forEach { (outcome, balance) ->
            val house = House()
            val accounting = HouseAccounting(house)
            val wallet = Wallet()
            accounting.collect(listOf(wallet to 10.0))
            assertEquals(990.0, wallet.balance)
            assertEquals(BigDecimal("100010.00"), house.chips)
            accounting.settle(listOf(HouseAccounting.Payout(wallet, 10.0, outcome)))
            assertEquals(balance, wallet.balance)
            assertEquals(101000.0, wallet.balance + house.chips.toDouble())
        }
    }

    @Test
    fun additionalWagersAndFullPayouts() {
        val house = House()
        val accounting = HouseAccounting(house)
        val wallet = Wallet()
        repeat(3) { accounting.collect(listOf(wallet to 10.0)) }
        assertEquals(970.0, wallet.balance)
        assertEquals(BigDecimal("100030.00"), house.chips)
        accounting.settle(listOf(
            HouseAccounting.Payout(wallet, 20.0, HouseAccounting.Outcome.WIN),
            HouseAccounting.Payout(wallet, 10.0, HouseAccounting.Outcome.PUSH)
        ))
        assertEquals(1020.0, wallet.balance)
        assertEquals(BigDecimal("99980.00"), house.chips)
    }

    @Test
    fun failedTransfersLeaveAllPlayerWalletsUnchangedAndCanRetry() {
        val house = House()
        val accounting = HouseAccounting(house)
        val first = Wallet()
        val second = Wallet()
        house.rejectDeposits = true
        assertThrows(IllegalStateException::class.java) {
            accounting.collect(listOf(first to 10.0, second to 10.0))
        }
        assertEquals(1000.0, first.balance)
        assertEquals(1000.0, second.balance)
        val payouts = listOf(first, second).map {
            HouseAccounting.Payout(it, 10.0, HouseAccounting.Outcome.WIN)
        }
        house.chips = BigDecimal("20.00")
        assertThrows(IllegalStateException::class.java) { accounting.settle(payouts) }
        assertEquals(1000.0, first.balance)
        assertEquals(1000.0, second.balance)
        assertEquals(BigDecimal("20.00"), house.chips)
        house.chips = BigDecimal("40.00")
        accounting.settle(payouts)
        assertEquals(1020.0, first.balance)
        assertEquals(1020.0, second.balance)
        assertEquals(BigDecimal("0.00"), house.chips)
    }

    @Test
    fun sharedTableTransfersWagersAndSettlementsWithoutBroadcastingHouseBalance() {
        val house = House()
        val manager = TableSessionManager(house, MemoryWallets(), leaderboard = MemoryLeaderboard())
        var state = ""
        val id = manager.joinTable(1, "player") { if (it.startsWith("STATE|")) state = it }
        fun playerBalance(): Double {
            val record = state.split('|')[6].split(',').first()
            return String(java.util.Base64.getUrlDecoder().decode(record)).split('~')[3].toDouble()
        }
        repeat(5) {
            assertTrue(manager.handle(id, "OPT_IN").startsWith("OK|"))
            assertTrue(manager.handle(id, "PLACE_BET", "10").startsWith("OK|"))
            assertTrue(manager.handle(id, "START_ROUND").startsWith("OK|"))
            assertEquals(101000.0, playerBalance() + house.chips.toDouble(), 0.000001)
            if (state.split('|')[2] == "PLAYERS") {
                assertTrue(manager.handle(id, "DOUBLE_DOWN").startsWith("OK|"))
            }
            assertEquals("FINISHED", state.split('|')[2])
            assertEquals(101000.0, playerBalance() + house.chips.toDouble(), 0.000001)
            assertEquals(7, state.split('|').size)
            assertFalse(state.contains("HOUSE_BALANCE"))
        }
        assertEquals("HOUSE_BALANCE|${house.chips.toPlainString()}", manager.houseBalance())
    }

    @Test
    fun fractionalCentBlackjackRoundsEquallyForBothWallets() {
        val house = House()
        val accounting = HouseAccounting(house)
        val wallet = Wallet()
        accounting.collect(listOf(wallet to 0.01))
        accounting.settle(listOf(HouseAccounting.Payout(wallet, 0.01, HouseAccounting.Outcome.BLACKJACK)))
        assertEquals(1000.02, wallet.balance, 0.000001)
        assertEquals(BigDecimal("99999.98"), house.chips)
    }
}
