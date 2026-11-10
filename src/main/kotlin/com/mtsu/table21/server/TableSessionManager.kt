package com.mtsu.table21.server

import com.mtsu.table21.game.BlackjackRules
import com.mtsu.table21.game.Shoe
import com.mtsu.table21.model.Bet
import com.mtsu.table21.model.Card
import com.mtsu.table21.model.Dealer
import com.mtsu.table21.model.Hand
import com.mtsu.table21.model.Player
import com.mtsu.table21.model.Wallet
import com.mtsu.table21.service.BettingService
import java.util.Base64
import java.util.UUID

/** Owns the authoritative shared table. All public operations are synchronized. */
class TableSessionManager {
    private data class ServerPlayer(
        val id: String,
        val seat: Int,
        val player: Player,
        val wallet: Wallet = Wallet(),
        var optedIn: Boolean = false,
        var bet: Double = 0.0,
        val handBets: MutableList<Double> = mutableListOf(),
        val doubledHands: MutableSet<Int> = mutableSetOf(),
        val doneHands: MutableSet<Int> = mutableSetOf(),
        var handIndex: Int = 0,
        var splitUsed: Boolean = false,
        var result: String = "",
        var connected: Boolean = true
    )

    private val shoe = Shoe(6)
    private val bettingService = BettingService()
    private val dealer = Dealer()
    private val players = arrayOfNulls<ServerPlayer>(4)
    private val writers = mutableMapOf<String, (String) -> Unit>()
    // Retained for the original TableSessionManager unit tests and older callers.
    private val legacyTables = mutableMapOf<String, MutableSet<String>>()
    private var roundNumber = 0
    private var phase = "WAITING" // WAITING, PLAYERS, DEALER, FINISHED
    private var currentSeat = -1
    private var dealerHidden = true

    @Synchronized
    fun joinTable(tableId: String, playerId: String) {
        legacyTables.getOrPut(tableId) { mutableSetOf() }.add(playerId)
    }

    @Synchronized
    fun leaveTable(tableId: String, playerId: String) {
        legacyTables[tableId]?.remove(playerId)
        if (legacyTables[tableId]?.isEmpty() == true) legacyTables.remove(tableId)
    }

    @Synchronized
    fun getPlayers(tableId: String): Set<String> = legacyTables[tableId]?.toSet() ?: emptySet()

    @Synchronized
    fun joinTable(username: String, send: (String) -> Unit): String {
        val cleanName = username.trim().take(18)
        if (cleanName.isBlank()) return "ERROR|Username cannot be empty"
        val existing = players.filterNotNull().firstOrNull { it.player.name.equals(cleanName, true) && it.connected }
        if (existing != null) return "ERROR|That username is already at the table"
        val seat = players.indexOfFirst { it == null }
        if (seat < 0) return "ERROR|Table is full (maximum 4 players)"
        val id = UUID.randomUUID().toString()
        val record = ServerPlayer(id, seat, Player(cleanName))
        players[seat] = record
        writers[id] = send
        send("JOINED|$id|$seat|${record.wallet.balance}")
        broadcastState()
        return id
    }

    @Synchronized
    fun leaveTable(playerId: String) {
        val p = findPlayer(playerId) ?: return
        writers.remove(playerId)
        if (phase == "PLAYERS" && !p.doneHands.contains(p.handIndex)) {
            p.doneHands.addAll(p.player.hands.indices)
            if (currentSeat == p.seat) advanceTurn()
        }
        players[p.seat] = null
        if (players.all { it == null }) resetToWaiting()
        broadcastState()
    }

    @Synchronized
    fun handle(playerId: String, command: String, argument: String = ""): String? {
        val p = findPlayer(playerId) ?: return "ERROR|Join a table first"
        return try {
            when (command) {
                "OPT_IN" -> {
                    requireWaiting("You can only opt in between rounds")
                    p.optedIn = true
                    p.result = "Ready"
                    "OK|You are playing this round"
                }
                "SIT_OUT" -> {
                    requireWaiting("You can only sit out between rounds")
                    p.optedIn = false
                    p.bet = 0.0
                    p.result = "Sitting out"
                    "OK|You will sit out this round"
                }
                "PLACE_BET" -> {
                    requireWaiting("Bets can only be changed between rounds")
                    val amount = argument.toDoubleOrNull() ?: throw IllegalArgumentException("Enter a valid bet")
                    require(amount.isFinite() && amount > 0.0) { "Bet must be greater than zero" }
                    require(p.wallet.canAfford(amount)) { "Not enough chips" }
                    p.bet = amount
                    "OK|Bet set to ${"%.2f".format(amount)}"
                }
                "START_ROUND" -> {
                    startRound()
                    "OK|Round started"
                }
                "HIT" -> { requireTurn(p); hit(p); "OK|Card dealt" }
                "STAND" -> { requireTurn(p); finishCurrentHand(p); "OK|You stood" }
                "DOUBLE_DOWN" -> { requireTurn(p); doubleDown(p); "OK|Doubled down" }
                "SPLIT" -> { requireTurn(p); split(p); "OK|Hand split" }
                else -> "ERROR|Unknown command"
            }
        } catch (e: Exception) {
            "ERROR|${e.message ?: "Command could not be completed"}"
        }.also { broadcastState() }
    }

    private fun requireWaiting(message: String) { require(phase == "WAITING" || phase == "FINISHED") { message } }
    private fun requireTurn(p: ServerPlayer) {
        require(phase == "PLAYERS") { "There is no active player round" }
        require(currentSeat == p.seat) { "It is not your turn" }
        require(!p.doneHands.contains(p.handIndex)) { "This hand is already finished" }
    }

    private fun startRound() {
        require(phase == "WAITING" || phase == "FINISHED") { "The current round is not finished" }
        val active = players.filterNotNull().filter { it.connected && it.optedIn }
        require(active.isNotEmpty()) { "At least one player must choose Play This Round" }
        active.forEach {
            require(it.bet > 0.0) { "${it.player.name} must place a bet" }
            require(it.wallet.canAfford(it.bet)) { "${it.player.name} cannot afford the bet" }
        }
        if (shoe.size() < 20) shoe.reset()
        dealer.resetHand()
        dealerHidden = true
        roundNumber++
        phase = "PLAYERS"
        currentSeat = -1
        players.filterNotNull().forEach { p ->
            p.player.resetHand()
            p.handBets.clear()
            p.doubledHands.clear()
            p.doneHands.clear()
            p.handIndex = 0
            p.splitUsed = false
            p.result = if (p.optedIn) "Playing" else "Sitting out"
            if (p.optedIn) {
                // BettingService removes the original wager from the player's wallet.
                val placedBet = bettingService.placeBet(p.wallet, p.bet)
                p.handBets.add(placedBet.amount)
            }
        }
        repeat(2) {
            active.forEach { it.player.addCard(drawCard()) }
            dealer.addCard(drawCard())
        }
        active.forEach { p ->
            if (p.player.getHand().isBlackjack) p.doneHands.add(0)
        }
        if (dealer.hand.isBlackjack || active.all { it.doneHands.contains(0) }) {
            playDealerAndSettle()
        } else {
            currentSeat = active.minOf { it.seat }
            skipFinishedPlayers()
        }
    }

    private fun hit(p: ServerPlayer) {
        val hand = p.player.getHand(p.handIndex)
        hand.addCard(drawCard())
        if (hand.isBust || hand.value == 21) finishCurrentHand(p)
    }

    private fun doubleDown(p: ServerPlayer) {
        val hand = p.player.getHand(p.handIndex)
        require(BlackjackRules.canDoubleDown(hand)) { "Double down is only allowed on the first two cards" }
        val extra = p.handBets[p.handIndex]
        require(p.wallet.canAfford(extra)) { "Not enough chips to double down" }
        p.wallet.removeChips(extra)
        p.doubledHands.add(p.handIndex)
        hand.addCard(drawCard())
        finishCurrentHand(p)
    }

    private fun split(p: ServerPlayer) {
        val hand = p.player.getHand(p.handIndex)
        require(!p.splitUsed) { "Only one split is allowed per round" }
        require(BlackjackRules.canSplit(hand)) { "You can only split matching ranks" }
        val extra = p.handBets[p.handIndex]
        require(p.wallet.canAfford(extra)) { "Not enough chips to split" }
        p.wallet.removeChips(extra)
        val secondCard = hand.removeCard(1)
        val secondHand = Hand()
        secondHand.addCard(secondCard)
        p.player.addHand(secondHand)
        p.handBets.add(extra)
        p.splitUsed = true
        hand.addCard(drawCard())
        secondHand.addCard(drawCard())
        if (hand.isBust || hand.value == 21) p.doneHands.add(p.handIndex)
        if (secondHand.isBust || secondHand.value == 21) p.doneHands.add(1)
        advanceWithinPlayerOrNext(p)
    }

    private fun finishCurrentHand(p: ServerPlayer) {
        p.doneHands.add(p.handIndex)
        advanceWithinPlayerOrNext(p)
    }

    private fun advanceWithinPlayerOrNext(p: ServerPlayer) {
        val nextHand = p.player.hands.indices.firstOrNull { !p.doneHands.contains(it) }
        if (nextHand != null) {
            p.handIndex = nextHand
            currentSeat = p.seat
        } else {
            p.result = "Finished"
            advanceTurn()
        }
    }

    private fun advanceTurn() {
        val next = players.filterNotNull()
            .filter { it.connected && it.optedIn && it.seat > currentSeat && it.doneHands.size < it.player.handCount }
            .minByOrNull { it.seat }
        if (next != null) {
            currentSeat = next.seat
            next.handIndex = next.player.hands.indices.firstOrNull { !next.doneHands.contains(it) } ?: 0
            return
        }
        playDealerAndSettle()
    }

    private fun skipFinishedPlayers() {
        val current = players.getOrNull(currentSeat)
        if (current == null || !current.optedIn || current.doneHands.size >= current.player.handCount) advanceTurn()
    }

    private fun playDealerAndSettle() {
        phase = "DEALER"
        dealerHidden = false
        while (BlackjackRules.shouldDealerHit(dealer.hand)) dealer.addCard(drawCard())
        players.filterNotNull().filter { it.optedIn }.forEach { p ->
            var anyWin = false
            p.player.hands.forEachIndexed { index, hand ->
                val wager = p.handBets[index] * if (p.doubledHands.contains(index)) 2.0 else 1.0
                // A 21 after splitting is an ordinary 21, not a natural blackjack.
                val result = if (p.player.handCount > 1 && hand.isBlackjack) {
                    when {
                        dealer.hand.isBust -> BlackjackRules.Result.PLAYER_WIN
                        hand.value > dealer.hand.value -> BlackjackRules.Result.PLAYER_WIN
                        hand.value < dealer.hand.value -> BlackjackRules.Result.DEALER_WIN
                        else -> BlackjackRules.Result.PUSH
                    }
                } else {
                    BlackjackRules.determineResult(hand, dealer.hand)
                }
                // A natural blackjack is exactly the original two-card hand.
                // It pays 3:2 only when it was not created by splitting and
                // the dealer does not also have a natural blackjack.
                val naturalBlackjack = hand.isBlackjack &&
                        hand.cardCount == 2 &&
                        !p.splitUsed &&
                        !dealer.hand.isBlackjack

                // BettingService handles all wallet payouts consistently.
                // The Bet passed here represents the full wager, including
                // any additional amount committed by doubling down.
                val settledBet = Bet(wager)
                when (result) {
                    BlackjackRules.Result.PLAYER_WIN -> {
                        if (naturalBlackjack) {
                            bettingService.payBlackjack(p.wallet, settledBet)
                        } else {
                            bettingService.payWin(p.wallet, settledBet)
                        }
                        anyWin = true
                    }
                    BlackjackRules.Result.PUSH -> bettingService.refundPush(p.wallet, settledBet)
                    BlackjackRules.Result.DEALER_WIN -> Unit
                }
            }
            p.result = if (anyWin) "Round complete - win" else "Round complete"
        }
        phase = "FINISHED"
        currentSeat = -1
        players.filterNotNull().forEach { it.optedIn = false }
    }

    private fun drawCard(): Card = shoe.dealCard() ?: run { shoe.reset(); shoe.dealCard()!! }

    private fun resetToWaiting() {
        phase = "WAITING"
        currentSeat = -1
        dealer.resetHand()
        dealerHidden = true
    }

    private fun findPlayer(id: String): ServerPlayer? = players.firstOrNull { it?.id == id }

    @Synchronized
    fun broadcastState() {
        val dealerCards = (0 until dealer.hand.cardCount).joinToString(",") { cardToken(dealer.hand.getCard(it)) }
        val currentId = players.getOrNull(currentSeat)?.id ?: "-"
        val records = players.filterNotNull().joinToString(",") { p ->
            val name64 = encode(p.player.name)
            val hands = p.player.hands.joinToString(";") { h -> (0 until h.cardCount).joinToString(",") { cardToken(h.getCard(it)) } }
            val fields = listOf(
                p.id, p.seat.toString(), name64, p.wallet.balance.toString(),
                p.optedIn.toString(), p.bet.toString(), p.result,
                p.handIndex.toString(), p.doneHands.sorted().joinToString("."), hands
            ).joinToString("~")
            encode(fields)
        }
        val line = listOf("STATE", roundNumber.toString(), phase, dealerCards,
            dealerHidden.toString(), currentId, records).joinToString("|")
        writers.values.toList().forEach { send ->
            try { send(line) } catch (_: Exception) { }
        }
    }

    private fun cardToken(card: Card) = "${card.rank.name}:${card.suit.name}"
    private fun encode(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
}
