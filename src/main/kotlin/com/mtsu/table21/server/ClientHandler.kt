package com.mtsu.table21.server

import java.net.Socket

class ClientHandler(
    private val socket: Socket,
    private val tableSessionManager: TableSessionManager
) : Runnable {
    override fun run() {
        var playerId: String? = null
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
                println("${socket.inetAddress.hostAddress}: $command")
                when (command) {
                    "PING" -> send("PONG")
                    "JOIN_TABLE" -> {
                        val username = try {
                            String(java.util.Base64.getUrlDecoder().decode(argument), Charsets.UTF_8)
                        } catch (_: Exception) { argument }
                        val joined = tableSessionManager.joinTable(username, ::send)
                        if (joined.startsWith("ERROR|")) send(joined) else playerId = joined
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
                            if (response != null) send(response)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Client handler ended: ${e.message}")
        } finally {
            playerId?.let { tableSessionManager.leaveTable(it) }
            try { socket.close() } catch (_: Exception) { }
            println("Client disconnected.")
        }
    }
}
