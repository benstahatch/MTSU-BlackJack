package com.mtsu.table21.server

import java.net.Socket

class ClientHandler(
    private val socket: Socket,
    private val tableSessionManager: TableSessionManager
) : Runnable {

    override fun run() {

        // close the connection when this client is done
        socket.use { client ->

            // set up reading and writing for this client
            val input = client.getInputStream().bufferedReader()
            val output = client.getOutputStream().bufferedWriter()
            val playerId = "player-${client.port}"

            output.write("CONNECTED\n")
            output.flush()

            var running = true
            var currentTableId: String? = null

            while (running) {

                // read one command at a time until the client disconnects
                val rawMessage = input.readLine() ?: break

                println("Received: $rawMessage")

                // convert the incoming text into a network message
                val message = try {
                    NetworkMessage.valueOf(
                        rawMessage.trim().uppercase()
                    )
                } catch (e: IllegalArgumentException) {
                    null
                }

                when (message) {

                    NetworkMessage.PING -> {
                        output.write("PONG\n")
                    }

                    // add the player to the default table
                    NetworkMessage.JOIN_TABLE -> {

                        val tableId = "table-1"

                        tableSessionManager.joinTable(
                            tableId,
                            playerId
                        )

                        currentTableId = tableId

                        output.write("JOINED $tableId\n")
                    }

                    // remove the player from their current table
                    NetworkMessage.LEAVE_TABLE -> {

                        val tableId = currentTableId

                        if (tableId != null) {

                            tableSessionManager.leaveTable(
                                tableId,
                                playerId
                            )

                            currentTableId = null

                            output.write("LEFT $tableId\n")

                        } else {

                            output.write("NOT IN A TABLE\n")
                        }
                    }

                    // acknowledge game commands for now
                    NetworkMessage.PLACE_BET -> {
                        output.write("PLACE_BET RECEIVED\n")
                    }

                    NetworkMessage.HIT -> {
                        output.write("HIT RECEIVED\n")
                    }

                    NetworkMessage.STAND -> {
                        output.write("STAND RECEIVED\n")
                    }

                    NetworkMessage.DOUBLE_DOWN -> {
                        output.write("DOUBLE_DOWN RECEIVED\n")
                    }

                    NetworkMessage.SPLIT -> {
                        output.write("SPLIT RECEIVED\n")
                    }

                    // stop reading commands when the player quits
                    NetworkMessage.QUIT -> {
                        output.write("BYE\n")
                        running = false
                    }

                    null -> {
                        output.write("UNKNOWN COMMAND\n")
                    }
                }

                // send the reply right away
                output.flush()
            }

            println("Client disconnected.")
        }
    }
}
