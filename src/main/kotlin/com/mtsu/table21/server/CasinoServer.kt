package com.mtsu.table21.server

import java.net.ServerSocket
import kotlin.concurrent.thread

private const val PORT = 5555

fun main() {

    // start the server on port 5555
    val serverSocket = ServerSocket(PORT)
    // share the table list between clients
    val tableSessionManager = TableSessionManager()


    println("Table21 Server")
    println("Listening on port $PORT...")

    while (true) {

        // wait for a client to connect
        val clientSocket = serverSocket.accept()

        println(
            "Client connected: ${clientSocket.inetAddress.hostAddress}"
        )

        // give each client its own thread
        thread {
            ClientHandler(clientSocket, tableSessionManager).run()
        }
    }
}
