package com.mtsu.table21.server

import java.net.Socket

class ClientHandler(
    private val socket: Socket,
    private val tableSessionManager: TableSessionManager,
    private val accounts: com.mtsu.table21.service.AccountService = com.mtsu.table21.service.AccountService(),
    private val wallets: com.mtsu.table21.database.WalletRepository = com.mtsu.table21.database.WalletRepository()
) : Runnable {
    override fun run() {
        var playerId: String? = null
        var authenticatedUser: com.mtsu.table21.model.User? = null
        val output = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
        val writeLock = Any()
        fun send(line: String) {
            synchronized(writeLock) {
                output.write(line)
                output.newLine()
                output.flush()
            }
        }
        try {
            socket.tcpNoDelay = true
            val input = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            send("CONNECTED")
            while (!socket.isClosed) {
                val raw = input.readLine() ?: break
                val parts = raw.split('|', limit = 2)
                val command = parts[0].trim().uppercase()
                val argument = parts.getOrElse(1) { "" }

                when (command) {
                    // todo: add server-side admin authorization.
                    "GET_HOUSE_BALANCE" -> send(tableSessionManager.houseBalance())
                    "PING" -> send("PONG")
                    "LOGIN", "REGISTER" -> {
                        if (authenticatedUser != null) {
                            send("AUTH_ERROR|Already authenticated; disconnect to switch accounts")
                            continue
                        }
                        // TODO: protect production credentials with encrypted transport/TLS.
                        // Base64 is protocol encoding only. Never log credentials or raw requests.
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
                                val name = java.util.Base64.getUrlEncoder().withoutPadding()
                                    .encodeToString(user.username.toByteArray(Charsets.UTF_8))
                                send("AUTH_OK|${user.id}|$name|${balance.toPlainString()}")
                            }
                        } catch (e: IllegalArgumentException) {
                            // Only service validation messages are safe; decoder errors are generic.
                            val safe = e.message?.takeIf { it in setOf("Username cannot be empty.",
                                "Username cannot exceed 50 characters.", "Password must contain at least 8 characters.",
                                "Username is already taken.") } ?: "Invalid credentials or encoding"
                            send("AUTH_ERROR|$safe")
                        } catch (_: Exception) {
                            send("AUTH_ERROR|Account service unavailable; please try again")
                        }
                    }
                    "JOIN_TABLE" -> {
                        val user = authenticatedUser
                        if (user == null) send("ERROR|Authentication required")
                        else if (playerId != null) send("ERROR|Already joined a table")
                        else {
                            val joined = tableSessionManager.joinTable(user.id, user.username, ::send)
                            if (joined.startsWith("ERROR|")) send(joined) else playerId = joined
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
        } catch (e: Exception) {
            println("Client connection ended.")
        } finally {
            playerId?.let { tableSessionManager.leaveTable(it) }
            try { socket.close() } catch (_: Exception) { }
            println("Client disconnected.")
        }
    }
}
