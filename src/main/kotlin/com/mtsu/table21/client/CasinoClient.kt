package com.mtsu.table21.client

import java.net.Socket

class CasinoClient(
    private val host: String = "127.0.0.1",
    private val port: Int = 5555
) {

    private var socket: Socket? = null

    private var input: java.io.BufferedReader? = null
    private var output: java.io.BufferedWriter? = null


    fun connect(): Boolean {

        if (socket?.isConnected == true) {
            return true
        }

        return try {

            socket = Socket(host, port)

            input =
                socket!!
                    .getInputStream()
                    .bufferedReader()

            output =
                socket!!
                    .getOutputStream()
                    .bufferedWriter()

            // Server sends CONNECTED immediately
            val response = input!!.readLine()

            response == "CONNECTED"

        } catch (e: Exception) {

            false
        }
    }


    fun sendCommand(command: String): String {

        val writer =
            output ?: return "NOT CONNECTED"

        val reader =
            input ?: return "NOT CONNECTED"

        writer.write("$command\n")
        writer.flush()

        return reader.readLine()
            ?: "NO RESPONSE"
    }


    fun disconnect() {

        socket?.close()

        socket = null
        input = null
        output = null
    }
}
