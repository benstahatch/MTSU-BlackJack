package com.mtsu.table21.server

import com.mtsu.table21.database.HouseWalletRepository
import com.mtsu.table21.database.WalletRepository
import com.mtsu.table21.game.Shoe
import com.mtsu.table21.model.Card
import com.mtsu.table21.model.Card.Rank.*
import com.mtsu.table21.model.Wallet
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

internal class MemoryWallets : WalletRepository() {
    val balances = mutableMapOf(1L to BigDecimal("1000.00"), 2L to BigDecimal("950.00"))
    var fail = false
    override fun findByUserId(userId: Long): Optional<Wallet> = Optional.ofNullable(balances[userId]?.let { Wallet(it.toDouble()) })
    override fun getBalance(userId: Long) = balances.getValue(userId)
    override fun setBalance(userId: Long, balance: BigDecimal) {
        check(!fail)
        check(balances.containsKey(userId))
        balances[userId] = balance
    }
}

internal class MemoryLeaderboard : com.mtsu.table21.database.LeaderboardRepository() {
    val hands = mutableListOf<HandResult>()
    val rounds = mutableListOf<String>()
    var bankrupt = false
    var fail = false
    override fun completePlayer(userId: Long, roundId: String, results: List<HandResult>): Boolean {
        check(!fail)
        hands.addAll(results)
        rounds.add(roundId)
        return bankrupt
    }
}

class PlayerWalletPersistenceTest {
    private class House : HouseWalletRepository() {
        var rejectPayouts = false
        var balanceValue = BigDecimal("100000.00")
        override fun addChips(amount: BigDecimal): Boolean { balanceValue += amount; return true }
        override fun removeChips(amount: BigDecimal): Boolean { if (rejectPayouts) return false; balanceValue -= amount; return true }
    }
    private fun shoe(vararg ranks: Card.Rank) = object : Shoe(6) {
        val cards = ranks.toMutableList()
        override fun size() = 52
        override fun dealCard() = Card(Card.Suit.HEARTS, cards.removeAt(0))
    }
    private fun start(manager: TableSessionManager, id: String) {
        assertTrue(manager.handle(id, "OPT_IN").startsWith("OK|"))
        assertTrue(manager.handle(id, "PLACE_BET", "10").startsWith("OK|"))
        assertTrue(manager.handle(id, "START_ROUND").startsWith("OK|"))
    }

    @Test fun selectingBetLeavesBalanceUnchangedUntilDealBroadcastsDeduction() {
        val wallets = MemoryWallets()
        val house = House()
        val manager = TableSessionManager(house, wallets, shoe(TEN, TEN, SIX, NINE), MemoryLeaderboard())
        var state = ""
        val id = manager.joinTable(1, "alice") { if (it.startsWith("STATE|")) state = it }
        fun displayedBalance(): Double {
            val record = String(java.util.Base64.getUrlDecoder().decode(state.split('|')[6]))
            return record.split('~')[3].toDouble()
        }
        manager.handle(id, "OPT_IN")
        manager.handle(id, "PLACE_BET", "10")
        assertEquals(1000.0, displayedBalance())
        assertEquals(BigDecimal("1000.00"), wallets.balances[1])
        assertEquals(BigDecimal("100000.00"), house.balanceValue)
        assertTrue(manager.handle(id, "START_ROUND").startsWith("OK|"))
        assertEquals(990.0, displayedBalance())
        assertEquals(BigDecimal("990.00"), wallets.balances[1])
        assertEquals(BigDecimal("100010.00"), house.balanceValue)
    }

    @Test fun consecutiveRoundsUseCurrentWagerAndRejectInvalidReplacement() {
        val wallets = MemoryWallets()
        wallets.balances[1] = BigDecimal("970.00")
        val house = House()
        val manager = TableSessionManager(house, wallets,
            shoe(TEN, TEN, SIX, NINE, TEN, TEN, SIX, NINE), MemoryLeaderboard())
        var state = ""
        val id = manager.joinTable(1, "alice") { if (it.startsWith("STATE|")) state = it }
        manager.handle(id, "OPT_IN")
        manager.handle(id, "PLACE_BET", "70")
        assertTrue(manager.handle(id, "START_ROUND", "500").startsWith("OK|"))
        assertEquals(BigDecimal("470.00"), wallets.balances[1])
        assertTrue(manager.handle(id, "STAND").startsWith("OK|"))
        assertEquals(BigDecimal("470.00"), wallets.balances[1])
        manager.handle(id, "OPT_IN")
        assertTrue(manager.handle(id, "START_ROUND").startsWith("ERROR|"))
        manager.handle(id, "PLACE_BET", "10")
        assertTrue(manager.handle(id, "START_ROUND", "500").startsWith("ERROR|"))
        assertEquals(BigDecimal("470.00"), wallets.balances[1])
        assertEquals("FINISHED", state.split('|')[2])
        assertTrue(manager.handle(id, "START_ROUND", "70").startsWith("OK|"))
        assertEquals(BigDecimal("400.00"), wallets.balances[1])
        val record = String(java.util.Base64.getUrlDecoder().decode(state.split('|')[6])).split('~')
        assertEquals(400.0, record[3].toDouble())
        assertEquals(70.0, record[5].toDouble())
        assertTrue(manager.handle(id, "STAND").startsWith("OK|"))
        assertEquals(BigDecimal("400.00"), wallets.balances[1])
        assertEquals(BigDecimal("100570.00"), house.balanceValue)
    }

    @Test fun seatedPlayerWithoutWagerNeitherBlocksTurnsNorBreaksSettlement() {
        val wallets = MemoryWallets()
        val scores = MemoryLeaderboard()
        val manager = TableSessionManager(House(), wallets, shoe(TEN, TEN, KING, EIGHT), scores)
        val alice = manager.joinTable(1, "alice") {}
        manager.joinTable(2, "bob") {} // seated and auto opted in, but never places a wager
        assertTrue(manager.handle(alice, "PLACE_BET", "10").startsWith("OK|"))
        assertTrue(manager.handle(alice, "START_ROUND").startsWith("OK|"))
        assertTrue(manager.handle(alice, "STAND").startsWith("OK|"))
        assertEquals(BigDecimal("1010.00"), wallets.balances[1])
        assertEquals(BigDecimal("950.00"), wallets.balances[2])
        assertEquals(1, scores.rounds.size)
    }

    @Test fun savedBalanceRejoinAndIndependentUsers() {
        val wallets = MemoryWallets()
        wallets.balances[1] = BigDecimal("1020.00")
        val manager = TableSessionManager(House(), wallets, leaderboard = MemoryLeaderboard())
        val messages = mutableListOf<String>()
        val first = manager.joinTable(1, "alice", messages::add)
        assertTrue(messages.first().endsWith("|1020.0"))
        assertTrue(manager.joinTable(1, "spoof") {}.startsWith("ERROR|"))
        manager.leaveTable(first)
        messages.clear()
        assertNotEquals(first, manager.joinTable(1, "alice", messages::add))
        assertTrue(messages.first().endsWith("|1020.0"))
        messages.clear()
        manager.joinTable(2, "bob", messages::add)
        assertTrue(messages.first().endsWith("|950.0"))
    }

    @Test fun lossWinPushAndBlackjackPersistAndConserveMoney() {
        listOf(
            listOf(TEN, TEN, SIX, NINE) to "990.00",
            listOf(TEN, TEN, KING, EIGHT) to "1010.00",
            listOf(TEN, TEN, EIGHT, EIGHT) to "1000.00",
            listOf(ACE, TEN, KING, EIGHT) to "1015.00"
        ).forEach { (cards, expected) ->
            val wallets = MemoryWallets()
            val house = House()
            val scores = MemoryLeaderboard()
            val manager = TableSessionManager(house, wallets, shoe(*cards.toTypedArray()), scores)
            val id = manager.joinTable(1, "alice") {}
            start(manager, id)
            if (cards.first() != ACE) {
                assertEquals(BigDecimal("990.00"), wallets.balances[1])
                assertTrue(manager.handle(id, "STAND").startsWith("OK|"))
            }
            assertEquals(BigDecimal(expected), wallets.balances[1])
            val recorded = scores.hands.single()
            assertEquals(BigDecimal("10.00"), recorded.wager())
            assertEquals(BigDecimal(expected).subtract(BigDecimal("990.00")), recorded.payout())
            assertEquals(when (expected) {
                "990.00" -> "DEALER_WIN"
                "1000.00" -> "PUSH"
                else -> "PLAYER_WIN"
            }, recorded.result())
            assertFalse(recorded.split())
            assertFalse(recorded.doubled())
            assertEquals(36, scores.rounds.single().length)
            assertEquals(BigDecimal("101000.00"), wallets.balances.getValue(1) + house.balanceValue)
            assertEquals(BigDecimal("950.00"), wallets.balances[2])
            manager.leaveTable(id)
            val messages = mutableListOf<String>()
            manager.joinTable(1, "alice", messages::add)
            assertTrue(messages.first().endsWith("|${expected.toDouble()}"))
        }
    }

    @Test fun doubleAndSplitPersistAdditionalWagers() {
        val wallets = MemoryWallets()
        val house = House()
        val scores = MemoryLeaderboard()
        val manager = TableSessionManager(house, wallets, shoe(EIGHT, TEN, EIGHT, SEVEN, THREE, TWO, TEN), scores)
        val id = manager.joinTable(1, "alice") {}
        start(manager, id)
        assertTrue(manager.handle(id, "SPLIT").startsWith("OK|"))
        assertEquals(BigDecimal("980.00"), wallets.balances[1])
        assertTrue(manager.handle(id, "DOUBLE_DOWN").startsWith("OK|"))
        assertEquals(BigDecimal("970.00"), wallets.balances[1])
        assertTrue(manager.handle(id, "STAND").startsWith("OK|"))
        assertEquals(BigDecimal("1010.00"), wallets.balances[1])
        assertEquals(BigDecimal("99990.00"), house.balanceValue)
        assertEquals(listOf(0, 1), scores.hands.map { it.handIndex() })
        assertEquals(listOf(BigDecimal("20.00"), BigDecimal("10.00")), scores.hands.map { it.wager() })
        assertTrue(scores.hands.all { it.split() })
        assertTrue(scores.hands[0].doubled())
        assertFalse(scores.hands[1].doubled())
        assertEquals(1, scores.rounds.size)
    }

    @Test fun failedPersistenceHaltsTableWithoutRepeatingTransfers() {
        val wallets = MemoryWallets()
        val house = House()
        val manager = TableSessionManager(house, wallets, leaderboard = MemoryLeaderboard())
        val id = manager.joinTable(1, "alice") {}
        manager.handle(id, "OPT_IN")
        manager.handle(id, "PLACE_BET", "10")
        wallets.fail = true
        assertTrue(manager.handle(id, "START_ROUND").contains("wallet persistence failed"))
        wallets.fail = false
        assertTrue(manager.handle(id, "START_ROUND").startsWith("ERROR|"))
        assertEquals(BigDecimal("100010.00"), house.balanceValue)
        manager.leaveTable(id)
        assertTrue(manager.joinTable(1, "alice") {}.startsWith("ERROR|"))
    }

    @Test fun disconnectDuringRoundSettlesAndRejoinLoadsSavedBalance() {
        val wallets = MemoryWallets()
        val manager = TableSessionManager(House(), wallets, shoe(TEN, TEN, KING, EIGHT), MemoryLeaderboard())
        val id = manager.joinTable(1, "alice") {}
        start(manager, id)
        manager.leaveTable(id)
        assertEquals(BigDecimal("1010.00"), wallets.balances[1])
        val messages = mutableListOf<String>()
        val rejoined = manager.joinTable(1, "alice", messages::add)
        assertNotEquals(id, rejoined)
        assertTrue(messages.first().endsWith("|1010.0"))
    }

    @Test fun failedPayoutWriteCannotPayTwice() {
        val wallets = MemoryWallets()
        val house = House()
        val manager = TableSessionManager(house, wallets, shoe(TEN, TEN, KING, EIGHT), MemoryLeaderboard())
        val id = manager.joinTable(1, "alice") {}
        start(manager, id)
        wallets.fail = true
        assertTrue(manager.handle(id, "STAND").contains("wallet persistence failed"))
        assertEquals(BigDecimal("99990.00"), house.balanceValue)
        wallets.fail = false
        assertTrue(manager.handle(id, "START_ROUND").startsWith("ERROR|"))
        assertEquals(BigDecimal("99990.00"), house.balanceValue)
    }

    @Test fun bankruptcyNotifiesAndRemovesPlayerOnlyAfterSettlement() {
        val wallets = MemoryWallets()
        wallets.balances[1] = BigDecimal("10.00")
        val scores = MemoryLeaderboard().apply { bankrupt = true }
        val messages = mutableListOf<String>()
        val manager = TableSessionManager(House(), wallets, shoe(TEN, TEN, SIX, NINE), scores)
        val id = manager.joinTable(1, "alice", messages::add)
        start(manager, id)
        assertFalse(messages.any { it.startsWith("BANKRUPT|") })
        manager.handle(id, "STAND")
        assertEquals("DEALER_WIN", scores.hands.single().result())
        assertEquals(1, messages.count { it.startsWith("BANKRUPT|") })
        assertEquals("ERROR|Join a table first", manager.handle(id, "OPT_IN"))
    }

    @Test fun historyFailureHaltsWithoutDuplicatePayout() {
        val wallets = MemoryWallets()
        val house = House()
        val scores = MemoryLeaderboard().apply { fail = true }
        val manager = TableSessionManager(house, wallets, shoe(TEN, TEN, KING, EIGHT), scores)
        val id = manager.joinTable(1, "alice") {}
        start(manager, id)
        assertTrue(manager.handle(id, "STAND").contains("history/score persistence failed"))
        scores.fail = false
        assertTrue(manager.handle(id, "START_ROUND").startsWith("ERROR|"))
        assertEquals(BigDecimal("1010.00"), wallets.balances[1])
        assertEquals(BigDecimal("99990.00"), house.balanceValue)
    }

    @Test fun settlementRetryRecordsHandsAndPaysOnlyOnce() {
        val wallets = MemoryWallets()
        val house = House().apply { rejectPayouts = true }
        val scores = MemoryLeaderboard()
        val manager = TableSessionManager(house, wallets, shoe(TEN, TEN, KING, EIGHT), scores)
        val id = manager.joinTable(1, "alice") {}
        start(manager, id)
        assertTrue(manager.handle(id, "STAND").startsWith("ERROR|"))
        assertTrue(scores.hands.isEmpty())
        house.rejectPayouts = false
        assertTrue(manager.handle(id, "START_ROUND").startsWith("OK|"))
        assertEquals(1, scores.hands.size)
        assertEquals(BigDecimal("1010.00"), wallets.balances[1])
        assertTrue(manager.handle(id, "START_ROUND").startsWith("ERROR|"))
        assertEquals(1, scores.hands.size)
        assertEquals(BigDecimal("1010.00"), wallets.balances[1])
    }

    @Test fun zeroBalanceCanJoinButCannotBetAndMissingWalletFails() {
        val wallets = MemoryWallets()
        wallets.balances[1] = BigDecimal("0.00")
        val manager = TableSessionManager(House(), wallets, leaderboard = MemoryLeaderboard())
        val id = manager.joinTable(1, "alice") {}
        assertFalse(id.startsWith("ERROR|"))
        assertTrue(manager.handle(id, "PLACE_BET", "1").startsWith("ERROR|"))
        assertTrue(manager.joinTable(3, "missing") {}.startsWith("ERROR|"))
    }
}
