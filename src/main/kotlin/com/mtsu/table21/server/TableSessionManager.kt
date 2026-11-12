package com.mtsu.table21.server

import com.mtsu.table21.game.BlackjackRules
import com.mtsu.table21.game.Shoe
import com.mtsu.table21.model.Card
import com.mtsu.table21.model.Dealer
import com.mtsu.table21.model.Hand
import com.mtsu.table21.model.Player
import com.mtsu.table21.model.Wallet
import com.mtsu.table21.database.HouseWalletRepository
import java.util.Base64
import java.util.UUID

/** Owns the authoritative shared table. All public operations are synchronized. */
class TableSessionManager(
    private val houseWallet: HouseWalletRepository = HouseWalletRepository(),
    private val wallets: com.mtsu.table21.database.WalletRepository = com.mtsu.table21.database.WalletRepository(),
    private val shoe: Shoe = Shoe(6)
) {
    private data class ServerPlayer(
        val userId: Long,
        val id: String,
        val seat: Int,
        val player: Player,
        val wallet: Wallet,
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

    private val accounting = HouseAccounting(houseWallet)
    private val dealer = Dealer()
    private val players = arrayOfNulls<ServerPlayer>(4)
    private val writers = mutableMapOf<String, (String) -> Unit>()
    // Retained for the original TableSessionManager unit tests and older callers.
    private val legacyTables = mutableMapOf<String, MutableSet<String>>()
    private var persistenceFailed = false
    private var roundNumber = 0
    private var phase = "WAITING" // WAITING, PLAYERS, DEALER, FINISHED, ERROR
    private var currentSeat = -1
    private var dealerHidden = true
    private var tableOpen = true

    @Synchronized
    fun houseBalance(): String = try {
        "HOUSE_BALANCE|${houseWallet.getBalance().toPlainString()}"
    } catch (_: Exception) {
        "ERROR|House balance unavailable"
    }

    @Synchronized
    fun adminStatus(): String = listOf(
        "ADMIN_STATUS", roundNumber.toString(), phase, currentSeat.toString(),
        dealer.hand.cardCount.toString(), players.count { it != null }.toString(),
        tableOpen.toString(), persistenceFailed.toString()
    ).joinToString("|")

    /** True only when no player is seated and the table is between rounds. */
    @Synchronized
    fun isEmptyForCleanup(): Boolean =
        players.all { it == null } && (phase == "WAITING" || phase == "FINISHED")

    @Synchronized
    fun adminPlayers(): String {
        val records = players.filterNotNull().joinToString(",") { p ->
            val fields = listOf(
                encode(p.player.name), p.seat.toString(), p.wallet.balance.toString(),
                p.bet.toString(), p.connected.toString(), p.result.ifBlank { "Waiting" }
            ).joinToString("~")
            encode(fields)
        }
        return "ADMIN_PLAYERS|$records"
    }

    @Synchronized
    fun setTableOpen(open: Boolean): String {
        if (phase == "PLAYERS" || phase == "DEALER") {
            return "ERROR|The table can only be opened or closed between rounds"
        }
        tableOpen = open
        return "ADMIN_TABLE|${if (tableOpen) "OPEN" else "CLOSED"}"
    }

    @Synchronized
    fun kickPlayer(username: String): String {
        if (phase == "PLAYERS" || phase == "DEALER") {
            return "ERROR|Players cannot be removed during an active round"
        }
        val target = players.filterNotNull().firstOrNull { it.player.name.equals(username, ignoreCase = true) }
            ?: return "ERROR|Player is not currently at the table"
        try { writers.remove(target.id)?.invoke("KICKED|An administrator removed you from the table") }
        catch (_: Exception) { writers.remove(target.id) }
        players[target.seat] = null
        broadcastState()
        return "ADMIN_KICKED|${target.player.name}"
    }

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
    fun joinTable(userId: Long, username: String, send: (String) -> Unit): String {
        if (!tableOpen) return "ERROR|Table is currently closed by an administrator"
        if (persistenceFailed) return "ERROR|Table halted: wallet persistence failed; contact server operator"
        val cleanName = username
        if (cleanName.isBlank()) return "ERROR|Username cannot be empty"
        val existing = players.filterNotNull().firstOrNull { it.userId == userId }
        if (existing != null) return "ERROR|This account already has a table session; wait for its round to finish"
        val seat = players.indexOfFirst { it == null }
        if (seat < 0) return "ERROR|Table is full (maximum 4 players)"
        val id = UUID.randomUUID().toString()
        val wallet = try {
            wallets.findByUserId(userId).orElseThrow { IllegalStateException("Missing wallet") }
        } catch (_: Exception) { return "ERROR|Player wallet unavailable" }
        // Players joining between rounds are automatically seated and opted in.
        // A player joining during an active round is seated for the next round.
        val autoOptIn = phase == "WAITING" || phase == "FINISHED"
        val record = ServerPlayer(
            userId, id, seat, Player(cleanName), wallet,
            optedIn = autoOptIn,
            result = if (autoOptIn) "Ready" else "Waiting for next round"
        )
        players[seat] = record
        writers[id] = send
        send("JOINED|$id|$seat|${record.wallet.balance}")
        broadcastState()
        return id
    }

    @Synchronized
    fun userIdForPlayer(playerId: String): Long? = findPlayer(playerId)?.userId

    @Synchronized
    fun leaveTable(playerId: String) {
        val p = findPlayer(playerId) ?: return
        writers.remove(playerId)
        p.connected = false
        if (!persistenceFailed && (phase == "PLAYERS" || phase == "DEALER")) {
            p.doneHands.addAll(p.player.hands.indices)
            if (currentSeat == p.seat) {
                try { advanceTurn() } catch (_: Exception) {
                    p.result = "Settlement pending"
                    writers.values.toList().forEach { try { it("ERROR|Settlement failed; contact server operator") } catch (_: Exception) { } }
                }
            }
        }
        if (phase == "WAITING" || phase == "FINISHED") players[p.seat] = null
        if (players.all { it == null }) resetToWaiting()
        broadcastState()
    }

    @Synchronized
    fun handle(playerId: String, command: String, argument: String = ""): String {
        val p = findPlayer(playerId) ?: return "ERROR|Join a table first"
        return try {
            check(!persistenceFailed) { "Table halted: wallet persistence failed; contact server operator" }
            when (command) {
                "REQUEST_STATE" -> {
                    broadcastState()
                    "OK|Table state refreshed"
                }
                "OPT_IN" -> {
                    requireWaiting("You can only opt in between rounds")
                    if (!p.optedIn) {
                        p.bet = 0.0
                        p.player.resetHand()
                    }
                    p.optedIn = true
                    p.result = "Ready"
                    "OK|Enter your wager, then press Deal to bet and start"
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
                    setBet(p, argument)
                    "OK|Bet set to ${"%.2f".format(p.bet)}; chips are deducted when dealt"
                }
                "START_ROUND" -> {
                    // A supplied wager is validated before starting; never fall back to an old bet.
                    if (argument.isNotEmpty()) {
                        requireWaiting("The current round is not finished")
                        require(p.optedIn) { "Choose Play This Round before placing a wager" }
                        setBet(p, argument)
                    }
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

    private fun setBet(p: ServerPlayer, argument: String) {
        val amount = argument.toDoubleOrNull() ?: throw IllegalArgumentException("Enter a valid bet")
        require(amount.isFinite() && amount > 0.0) { "Bet must be greater than zero" }
        require(p.wallet.canAfford(amount)) { "Not enough chips" }
        require(java.math.BigDecimal.valueOf(amount).stripTrailingZeros().scale() <= 2) { "Use whole cents for bets" }
        p.bet = amount
    }

    private fun requireWaiting(message: String) { require(phase == "WAITING" || phase == "FINISHED") { message } }
    private fun requireTurn(p: ServerPlayer) {
        require(phase == "PLAYERS") { "There is no active player round" }
        require(currentSeat == p.seat) { "It is not your turn" }
        require(!p.doneHands.contains(p.handIndex)) { "This hand is already finished" }
    }

    private fun startRound() {
        if (phase == "DEALER") {
            playDealerAndSettle()
            return
        }
        require(phase == "WAITING" || phase == "FINISHED") { "The current round is not finished" }
        // An opted-in player is eligible only after entering a valid wager.
        // This prevents a newly seated player with no bet from blocking everyone else.
        val active = players.filterNotNull().filter { it.connected && it.optedIn && it.bet > 0.0 }
        require(active.isNotEmpty()) { "At least one seated player must place a bet before the round can start" }
        active.forEach {
            require(it.bet > 0.0) { "${it.player.name} must place a bet" }
            require(it.wallet.canAfford(it.bet)) { "${it.player.name} cannot afford the bet" }
        }
        accounting.collect(active.map { it.wallet to it.bet })
        persist(active)
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
            val isPlayingThisRound = p in active
            p.result = if (isPlayingThisRound) "Playing" else "Sitting out"
            if (isPlayingThisRound) {
                p.handBets.add(p.bet)
            }
        }
        repeat(2) {
            active.forEach { it.player.addCard(drawCard()) }
            dealer.addCard(drawCard())
        }
        active.forEach { p ->
            if (p.player.hand.isBlackjack) p.doneHands.add(0)
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
        accounting.collect(listOf(p.wallet to extra))
        persist(listOf(p))
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
        accounting.collect(listOf(p.wallet to extra))
        persist(listOf(p))
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
        val payouts = mutableListOf<HouseAccounting.Payout>()
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

                val outcome = when (result) {
                    BlackjackRules.Result.PLAYER_WIN -> {
                        anyWin = true
                        if (naturalBlackjack) HouseAccounting.Outcome.BLACKJACK else HouseAccounting.Outcome.WIN
                    }
                    BlackjackRules.Result.PUSH -> HouseAccounting.Outcome.PUSH
                    BlackjackRules.Result.DEALER_WIN -> HouseAccounting.Outcome.LOSS
                }
                payouts.add(HouseAccounting.Payout(p.wallet, wager, outcome))
            }
            p.result = if (anyWin) "Round complete - win" else "Round complete"
        }
        accounting.settle(payouts)
        persist(players.filterNotNull().filter { it.optedIn })
        phase = "FINISHED"
        currentSeat = -1
        players.filterNotNull().forEach {
            it.optedIn = false
            if (!it.connected) players[it.seat] = null
        }
    }

    // House transfers and player writes are not one durable transaction. On a failed
    // write halt this table, retaining identities and balances for operator reconciliation.
    private fun persist(changed: List<ServerPlayer>) {
        try {
            changed.forEach {
                wallets.setBalance(it.userId, java.math.BigDecimal.valueOf(it.wallet.balance)
                    .setScale(2, java.math.RoundingMode.HALF_UP))
            }
        } catch (_: Exception) {
            persistenceFailed = true
            phase = "ERROR"
            throw IllegalStateException("Table halted: wallet persistence failed; contact server operator")
        }
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
