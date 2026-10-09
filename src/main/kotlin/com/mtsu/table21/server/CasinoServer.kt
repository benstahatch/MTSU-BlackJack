package com.mtsu.table21.server

import java.net.ServerSocket
import kotlin.concurrent.thread

private const val PORT = 5555

fun main() {

    val serverSocket = ServerSocket(PORT)

    println("Table21 Server")
    println("Listening on port $PORT...")

    while (true) {

        val clientSocket = serverSocket.accept()

        println(
            "Client connected: ${clientSocket.inetAddress.hostAddress}"
        )

        thread {
            ClientHandler(clientSocket).run()
        }
    }
}
