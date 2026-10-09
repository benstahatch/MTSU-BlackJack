package com.mtsu.table21.server

import java.net.Socket

class ClientHandler(
    private val socket: Socket
) : Runnable {

    override fun run() {

        socket.use { client ->

            val input = client.getInputStream().bufferedReader()
            val output = client.getOutputStream().bufferedWriter()

            output.write("CONNECTED\n")
            output.flush()

            var running = true

            while (running) {

                val message = input.readLine() ?: break

                println("Received: $message")

                when (message.trim().uppercase()) {

                    "PING" -> {
                        output.write("PONG\n")
                    }

                    "QUIT" -> {
                        output.write("BYE\n")
                        running = false
                    }

                    else -> {
                        output.write("UNKNOWN COMMAND\n")
                    }
                }

                output.flush()
            }

            println("Client disconnected.")
        }
    }
}
