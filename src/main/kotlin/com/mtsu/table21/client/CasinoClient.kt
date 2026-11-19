package com.mtsu.table21.client

import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/** TCP client. One background thread reads server broadcasts; UI code never blocks waiting for a reply. */
class CasinoClient(
    private val host: String = serverHost(System.getenv()),
    private val port: Int = serverPort(System.getenv())
) {
    companion object {
        internal fun serverHost(environment: Map<String, String>): String =
            environment["TABLE21_SERVER_HOST"]?.trim()?.takeIf { it.isNotEmpty() } ?: "127.0.0.1"

        internal fun serverPort(environment: Map<String, String>): Int =
            environment["TABLE21_SERVER_PORT"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 5555
    }

    @Volatile var authenticatedUserId: Long? = null
        private set
    @Volatile var authenticatedUsername: String? = null
        private set
    @Volatile var isAdmin: Boolean = false
        private set
    private var sessionPlayerId: String? = null

    fun login(username: String, password: String) = authenticate("LOGIN", username, password)
    fun register(username: String, password: String) = authenticate("REGISTER", username, password)
    private fun authenticate(command: String, username: String, password: String): String {
        fun encode(value: String) = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(Charsets.UTF_8))
        return sendCommand("$command|${encode(username)}|${encode(password)}")
    }

    private fun clearSession() {
        authenticatedUserId = null
        authenticatedUsername = null
        isAdmin = false
        playerBalance = null
        sessionPlayerId = null
    }

    @Volatile var playerBalance: Double? = null
        private set

    fun updatePlayerBalance(balance: Double) {
        if (balance.isFinite() && balance >= 0) playerBalance = balance
    }

    @Volatile private var socket: Socket? = null
    @Volatile private var input: BufferedReader? = null
    @Volatile private var output: BufferedWriter? = null
    @Volatile private var listener: ((String) -> Unit)? = null

    // Additional listeners for admin screens
    private val adminListeners =
        java.util.concurrent.CopyOnWriteArrayList<(String) -> Unit>()

    private val connected = AtomicBoolean(false)
    private val writeLock = Any()

    @Volatile private var bankruptcyListener: (() -> Unit)? = null
    fun setBankruptcyListener(callback: (() -> Unit)?) { bankruptcyListener = callback }

    fun setMessageListener(callback: ((String) -> Unit)?) {
        listener = callback
    }

    fun addAdminListener(callback: (String) -> Unit) {
        adminListeners.add(callback)
    }

    fun removeAdminListener(callback: (String) -> Unit) {
        adminListeners.remove(callback)
    }

    @Synchronized
    fun connect(): Boolean {
        if (connected.get() && socket?.isClosed == false) return true
        return try {
            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(host, port), 4000)
            newSocket.tcpNoDelay = true
            val newInput = newSocket.getInputStream().bufferedReader(Charsets.UTF_8)
            val newOutput = newSocket.getOutputStream().bufferedWriter(Charsets.UTF_8)
            val greeting = newInput.readLine()
            if (greeting != "CONNECTED") {
                newSocket.close()
                false
            } else {
                socket = newSocket
                input = newInput
                output = newOutput
                connected.set(true)
                Thread({ readLoop(newInput, newSocket) }, "Table21-client-reader").apply {
                    isDaemon = true
                    start()
                }
                true
            }
        } catch (_: Exception) {
            disconnect()
            false
        }
    }

    private fun readLoop(reader: BufferedReader, connection: Socket) {
        try {
            while (connected.get() && socket === connection) {
                val line = reader.readLine() ?: break
                if (line.startsWith("BANKRUPT|")) {
                    clearSession()
                    bankruptcyListener?.invoke()
                    listener?.invoke(line)
                    adminListeners.forEach { it(line) }
                    break
                }
                if (line.startsWith("AUTH_OK|")) {
                    val fields = line.split('|')
                    authenticatedUserId = fields[1].toLong()
                    authenticatedUsername = String(java.util.Base64.getUrlDecoder().decode(fields[2]), Charsets.UTF_8)
                    updatePlayerBalance(fields[3].toDouble())
                    isAdmin = fields.getOrNull(4) == "ADMIN"
                }
                if (line.startsWith("STATE|")) {
                    line.split('|').getOrNull(6)?.split(',')?.filter { it.isNotEmpty() }?.forEach {
                        val fields = String(java.util.Base64.getUrlDecoder().decode(it), Charsets.UTF_8).split('~')
                        if (fields[0] == sessionPlayerId) updatePlayerBalance(fields[3].toDouble())
                    }
                }
                if (line.startsWith("JOINED|")) {
                    sessionPlayerId = line.split('|')[1]
                    line.split('|').getOrNull(3)?.toDoubleOrNull()?.let(::updatePlayerBalance)
                }
                listener?.invoke(line)
                adminListeners.forEach { it(line) }
            }
        } catch (_: Exception) {
            // A closed socket ends the reader loop.
        } finally {
            synchronized(this) {
                if (socket === connection) {
                    disconnect()
                    listener?.invoke("DISCONNECTED")
                    adminListeners.forEach { it("DISCONNECTED") }
                }
            }
        }
    }

    /** Sends one protocol line. Replies and state changes arrive through setMessageListener. */
    fun sendCommand(command: String): String {
        val writer = output ?: return "NOT CONNECTED"
        if (!connected.get()) return "NOT CONNECTED"
        return try {
            synchronized(writeLock) {
                writer.write(command.replace("\n", "").replace("\r", ""))
                writer.newLine()
                writer.flush()
            }
            "SENT"
        } catch (e: Exception) {
            "SEND FAILED: ${e.message ?: "connection error"}"
        }
    }

    fun isConnected(): Boolean = connected.get() && socket?.isClosed == false

    @Synchronized
    fun disconnect() {
        connected.set(false)
        clearSession()
        try { socket?.close() } catch (_: Exception) { }
        socket = null
        input = null
        output = null
    }
}
