package com.mtsu.table21.server

import java.net.Socket

class ClientHandler(
    private val socket: Socket,
    private val tableSessionManager: MultiTableSessionManager,
    private val accounts: com.mtsu.table21.service.AccountService = com.mtsu.table21.service.AccountService(),
    private val wallets: com.mtsu.table21.database.WalletRepository = com.mtsu.table21.database.WalletRepository()
) : Runnable {
    /** Keeps compatibility with tests and older callers that provide a single-table manager. */
    constructor(socket: Socket, tableSessionManager: TableSessionManager) : this(
        socket, MultiTableSessionManager(tableSessionManager)
    )

    override fun run() {
        var playerId: String? = null
        var authenticatedUser: com.mtsu.table21.model.User? = null
        var adminAuthorized = false
        var metricsOpened = false
        val connectionId = "${socket.inetAddress.hostAddress}:${socket.port}"
        lateinit var output: java.io.BufferedWriter
        val writeLock = Any()
        fun send(line: String) {
            synchronized(writeLock) {
                output.write(line)
                output.newLine()
                output.flush()
                if (line.startsWith("BANKRUPT|")) socket.close()
            }
        }
        try {
            ServerMetrics.connectionOpened()
            metricsOpened = true
            output = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
            socket.tcpNoDelay = true
            val input = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            send("CONNECTED")
            while (!socket.isClosed) {
                val raw = input.readLine() ?: break
                val parts = raw.split('|', limit = 2)
                val command = parts[0].trim().uppercase()
                val argument = parts.getOrElse(1) { "" }

                // Other sockets authenticated to a deleted account lose access on their next request.
                if (authenticatedUser != null && command !in setOf("QUIT", "PING")) {
                    val exists = try { accounts.findUser(authenticatedUser.id).isPresent }
                    catch (_: Exception) { send("ERROR|Account service unavailable"); continue }
                    if (!exists) { send(TableSessionManager.BANKRUPT_MESSAGE); break }
                }

                when (command) {
                    "PING" -> send("PONG")
                    "GET_TABLE_LIST" -> {
                        if (authenticatedUser == null) send("ERROR|Authentication required")
                        else tableSessionManager.subscribeToTableList(connectionId, ::send)
                    }
                    "GET_HOUSE_BALANCE", "GET_ADMIN_STATUS", "GET_ADMIN_TABLES", "GET_ADMIN_PLAYERS", "GET_ADMIN_SERVER" -> {
                        if (!adminAuthorized) send("ERROR|Administrator access required")
                        else when (command) {
                            "GET_HOUSE_BALANCE" -> send(tableSessionManager.houseBalance())
                            "GET_ADMIN_STATUS" -> send(tableSessionManager.adminStatus(argument.ifBlank { "1" }))
                            "GET_ADMIN_TABLES" -> send(tableSessionManager.adminTables())
                            "GET_ADMIN_PLAYERS" -> send(tableSessionManager.adminPlayers(argument.ifBlank { "1" }))
                            "GET_ADMIN_SERVER" -> send(ServerMetrics.info(5555))
                        }
                    }
                    "SET_TABLE_OPEN" -> {
                        if (!adminAuthorized) send("ERROR|Administrator access required")
                        else {
                            val fields = argument.split('|', limit = 2)
                            val tableId = if (fields.size == 2) fields[0] else "1"
                            val openValue = if (fields.size == 2) fields[1] else fields[0]
                            send(tableSessionManager.setTableOpen(tableId, openValue.equals("true", true)))
                        }
                    }
                    "ADMIN_KICK" -> {
                        if (!adminAuthorized) send("ERROR|Administrator access required")
                        else send("ERROR|Use the table-specific player management panel to remove a player")
                    }
                    "LOGIN", "REGISTER" -> {
                        if (authenticatedUser != null) {
                            send("AUTH_ERROR|Already authenticated; disconnect to switch accounts")
                            continue
                        }
                        try {
                            val credentials = argument.split('|')
                            require(credentials.size == 2) { "Invalid credential encoding" }
                            fun decode(value: String) = String(java.util.Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
                            val username = decode(credentials[0])
                            val password = decode(credentials[1])
                            val user = if (command == "REGISTER") accounts.register(username, password)
                                else accounts.login(username, password).orElse(null)
                            if (user == null) send("AUTH_ERROR|Invalid username or password")
                            else {
                                val balance = wallets.getBalance(user.id)
                                authenticatedUser = user
                                val adminIds = System.getenv("TABLE21_ADMIN_USER_IDS").orEmpty()
                                    .split(',').mapNotNull { it.trim().toLongOrNull() }
                                adminAuthorized = user.id in adminIds
                                val name = java.util.Base64.getUrlEncoder().withoutPadding()
                                    .encodeToString(user.username.toByteArray(Charsets.UTF_8))
                                send("AUTH_OK|${user.id}|$name|${balance.toPlainString()}|${if (adminAuthorized) "ADMIN" else "PLAYER"}")
                            }
                        } catch (e: IllegalArgumentException) {
                            val safe = e.message?.takeIf { it in setOf("Username cannot be empty.",
                                "Username cannot exceed 50 characters.", "Password must contain at least 8 characters.",
                                "Username is already taken.") } ?: "Invalid credentials or encoding"
                            send("AUTH_ERROR|$safe")
                        } catch (_: Exception) {
                            send("AUTH_ERROR|Account service unavailable; please try again")
                        }
                    }
                    "GET_LEADERBOARD" -> {
                        if (authenticatedUser == null) send("ERROR|Authentication required")
                        else send(tableSessionManager.leaderboardResponse())
                    }
                    "CREATE_TABLE" -> {
                        val user = authenticatedUser
                        if (user == null) send("ERROR|Authentication required")
                        else if (playerId != null) send("ERROR|Leave your current table before creating another")
                        else {
                            var createdPlayerId: String? = null
                            val result = tableSessionManager.createTable(user.id, user.username) { line ->
                                if (line.startsWith("JOINED|")) createdPlayerId = line.split('|').getOrNull(1)
                                send(line)
                            }
                            if (result.startsWith("ERROR|")) send(result)
                            else {
                                playerId = createdPlayerId
                                tableSessionManager.unsubscribeFromTableList(connectionId)
                            }
                        }
                    }
                    "JOIN_TABLE" -> {
                        val user = authenticatedUser
                        if (user == null) send("ERROR|Authentication required")
                        else if (playerId != null) send("ERROR|Already joined a table")
                        else {
                            val tableId = argument.trim().ifBlank { "1" }
                            val joined = tableSessionManager.joinTable(tableId, user.id, user.username, ::send)
                            if (joined.startsWith("ERROR|")) send(joined) else {
                                playerId = joined
                                tableSessionManager.unsubscribeFromTableList(connectionId)
                            }
                        }
                    }
                    "LEAVE_TABLE" -> {
                        playerId?.let { tableSessionManager.leaveTable(it) }
                        playerId = null
                        send("LEFT")
                    }
                    "QUIT" -> {
                        playerId?.let { tableSessionManager.leaveTable(it) }
                        send("BYE")
                        break
                    }
                    else -> {
                        val id = playerId
                        if (id == null) send("ERROR|Join a table first")
                        else {
                            val response = tableSessionManager.handle(id, command, argument)
                            send(response)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            println("Client connection ended.")
        } finally {
            playerId?.let { tableSessionManager.leaveTable(it) }
            tableSessionManager.unsubscribeFromTableList(connectionId)
            try { socket.close() } catch (_: Exception) { }
            if (metricsOpened) ServerMetrics.connectionClosed()
            println("Client disconnected.")
        }
    }

}
