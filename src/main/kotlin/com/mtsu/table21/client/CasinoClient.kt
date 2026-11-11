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

    @Volatile var playerBalance: Double? = null
        private set

    fun updatePlayerBalance(balance: Double) {
        if (balance.isFinite() && balance >= 0) playerBalance = balance
    }

    @Volatile private var socket: Socket? = null
    @Volatile private var input: BufferedReader? = null
    @Volatile private var output: BufferedWriter? = null
    @Volatile private var listener: ((String) -> Unit)? = null
    private val connected = AtomicBoolean(false)
    private val writeLock = Any()

    fun setMessageListener(callback: ((String) -> Unit)?) {
        listener = callback
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
                Thread({ readLoop(newInput) }, "Table21-client-reader").apply {
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

    private fun readLoop(reader: BufferedReader) {
        try {
            while (connected.get()) {
                val line = reader.readLine() ?: break
                if (line.startsWith("JOINED|")) {
                    line.split('|').getOrNull(3)?.toDoubleOrNull()?.let(::updatePlayerBalance)
                }
                listener?.invoke(line)
            }
        } catch (_: Exception) {
            // A closed socket ends the reader loop.
        } finally {
            connected.set(false)
            listener?.invoke("DISCONNECTED")
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
        try { socket?.close() } catch (_: Exception) { }
        socket = null
        input = null
        output = null
    }
}
