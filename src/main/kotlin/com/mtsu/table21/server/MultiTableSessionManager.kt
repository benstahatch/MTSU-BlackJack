package com.mtsu.table21.server

import com.mtsu.table21.database.HouseWalletRepository
import com.mtsu.table21.database.WalletRepository
import com.mtsu.table21.game.Shoe
import java.util.Base64

/** Coordinates independent blackjack sessions and maps each connected player to one table. */
class MultiTableSessionManager private constructor(
    private val houseWallet: HouseWalletRepository,
    private val wallets: WalletRepository,
    initialTable: TableSessionManager?
) {
    constructor() : this(HouseWalletRepository(), WalletRepository(), null)
    constructor(initialTable: TableSessionManager) : this(HouseWalletRepository(), WalletRepository(), initialTable)
    private data class TableEntry(val id: String, val number: Int, val session: TableSessionManager)

    private val tables = linkedMapOf<String, TableEntry>()
    private val playerTables = mutableMapOf<String, String>()
    private val userTables = mutableMapOf<Long, String>()
    private val lobbyListeners = mutableMapOf<String, (String) -> Unit>()
    private var nextTableNumber = 2

    init {
        val firstTable = initialTable ?: TableSessionManager(houseWallet, wallets, Shoe(6))
        tables["1"] = TableEntry("1", 1, firstTable)
    }

    @Synchronized
    fun houseBalance(): String = tables["1"]!!.session.houseBalance()

    @Synchronized
    fun leaderboardResponse(): String = tables["1"]!!.session.leaderboardResponse()

    @Synchronized
    fun subscribeToTableList(connectionId: String, send: (String) -> Unit) {
        lobbyListeners[connectionId] = send
        send(tableList())
    }

    @Synchronized
    fun unsubscribeFromTableList(connectionId: String) {
        lobbyListeners.remove(connectionId)
    }

    @Synchronized
    private fun broadcastTableList() {
        val snapshot = tableList()
        lobbyListeners.values.toList().forEach { callback ->
            try { callback(snapshot) } catch (_: Exception) { }
        }
    }

    @Synchronized
    fun tableList(): String {
        closeEmptyTablesIfOverLimit()
        val records = tables.values.joinToString(",") { table ->
            val fields = tableFields(table)
            encode(fields)
        }
        return "TABLE_LIST|$records"
    }

    @Synchronized
    fun adminTables(): String {
        val removedTables = closeEmptyTablesIfOverLimit()
        if (removedTables) broadcastTableList()
        val records = tables.values.joinToString(",") { table -> encode(tableFields(table, admin = true)) }
        return "ADMIN_TABLES|$records"
    }

    private fun tableFields(table: TableEntry, admin: Boolean = false): String {
        val status = table.session.adminStatus().split('|')
        val round = status.getOrNull(1) ?: "0"
        val phase = status.getOrNull(2) ?: "WAITING"
        val currentSeat = status.getOrNull(3) ?: "-1"
        val dealerCards = status.getOrNull(4) ?: "0"
        val players = status.getOrNull(5) ?: "0"
        val open = status.getOrNull(6) ?: "true"
        val persistenceError = status.getOrNull(7) ?: "false"
        return if (admin) listOf(
            table.id, table.number, round, phase, currentSeat, dealerCards,
            players, open, persistenceError
        ).joinToString("~") else listOf(
            table.id, table.number, players, open, phase
        ).joinToString("~")
    }

    @Synchronized
    fun createTable(userId: Long, username: String, send: (String) -> Unit): String {
        if (userTables.containsKey(userId)) return "ERROR|You are already seated at a table"
        val number = nextTableNumber++
        val id = number.toString()
        val session = TableSessionManager(houseWallet, wallets, Shoe(6))
        val entry = TableEntry(id, number, session)
        tables[id] = entry
        val joined = joinInto(entry, userId, username, send)
        if (joined.first.startsWith("ERROR|")) {
            tables.remove(id)
            return joined.first
        }
        send("TABLE_CREATED|$id|$number")
        send("TABLE_JOINED|$id|$number")
        joined.second?.let(send)
        broadcastTableList()
        return "TABLE_CREATED|$id|$number"
    }

    @Synchronized
    fun joinTable(tableId: String, userId: Long, username: String, send: (String) -> Unit): String {
        if (userTables.containsKey(userId)) return "ERROR|You are already seated at a table. Leave it before joining another."
        val table = tables[tableId] ?: return "ERROR|That table no longer exists"
        val result = joinInto(table, userId, username, send)
        if (!result.first.startsWith("ERROR|")) {
            send("TABLE_JOINED|${table.id}|${table.number}")
            result.second?.let(send)
            broadcastTableList()
        }
        return result.first
    }

    /** Holds the initial state snapshot until the client has switched from lobby to table view. */
    private fun joinInto(
        table: TableEntry,
        userId: Long,
        username: String,
        send: (String) -> Unit
    ): Pair<String, String?> {
        var joining = true
        var initialState: String? = null
        val relay: (String) -> Unit = { line ->
            if (joining && line.startsWith("STATE|")) initialState = line else send(line)
        }
        val result = table.session.joinTable(userId, username, relay)
        joining = false
        if (result.startsWith("ERROR|")) return result to null
        playerTables[result] = table.id
        userTables[userId] = table.id
        return result to initialState
    }

    @Synchronized
    fun leaveTable(playerId: String) {
        val tableId = playerTables.remove(playerId) ?: return
        val table = tables[tableId] ?: return
        val userId = table.session.userIdForPlayer(playerId)
        table.session.leaveTable(playerId)
        if (userId != null) userTables.remove(userId)
        closeEmptyTablesIfOverLimit()
        broadcastTableList()
    }

    @Synchronized
    fun handle(playerId: String, command: String, argument: String = ""): String {
        val tableId = playerTables[playerId] ?: return "ERROR|Join a table first"
        val table = tables[tableId] ?: return "ERROR|Your table is no longer available"
        val response = table.session.handle(playerId, command, argument)

        // A player who disconnected during a round is kept until settlement finishes.
        // Recheck here because the table may only become empty after that round ends.
        if (closeEmptyTablesIfOverLimit()) broadcastTableList()
        return response
    }

    @Synchronized
    fun adminStatus(tableId: String = "1"): String = tables[tableId]?.session?.adminStatus()
        ?: "ERROR|Table not found"

    @Synchronized
    fun adminPlayers(tableId: String = "1"): String = tables[tableId]?.session?.adminPlayers()
        ?: "ERROR|Table not found"

    @Synchronized
    fun setTableOpen(tableId: String, open: Boolean): String = tables[tableId]?.session?.setTableOpen(open)
        ?: "ERROR|Table not found"

    @Synchronized
    fun setFirstTableOpen(open: Boolean): String = setTableOpen("1", open)

    /**
     * Keep the original Table 1 and at least three total tables. When the server
     * has more than three tables, remove empty non-default tables until the
     * count is back to three or no eligible empty table remains.
     *
     * The table session decides whether it is truly empty; disconnected players
     * in an unfinished round are therefore not removed prematurely.
     */
    @Synchronized
    private fun closeEmptyTablesIfOverLimit(): Boolean {
        if (tables.size <= 3) return false

        val emptyTables = tables.values
            .filter { it.id != "1" && it.session.isEmptyForCleanup() }
            .sortedByDescending { it.number }

        var removedAny = false
        for (table in emptyTables) {
            if (tables.size <= 3) break
            if (tables.remove(table.id) != null) removedAny = true
        }
        return removedAny
    }

    private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(Charsets.UTF_8))
}
