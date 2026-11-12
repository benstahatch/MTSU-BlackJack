package com.mtsu.table21.server

import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

private const val PORT = 5555

fun main() {
    ServerMetrics.serverStarted()
    val tableSessionManager = MultiTableSessionManager()
    ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0")).use { serverSocket ->
        println("Table21 Server")
        println("Listening on port $PORT on all network interfaces...")
        while (true) {
            val clientSocket = serverSocket.accept()
            println("Client connected: ${clientSocket.inetAddress.hostAddress}")
            thread(name = "Table21-${clientSocket.port}", isDaemon = true) {
                ClientHandler(clientSocket, tableSessionManager).run()
            }
        }
    }
}
