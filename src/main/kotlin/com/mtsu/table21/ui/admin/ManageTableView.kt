package com.mtsu.table21.ui.admin

import com.mtsu.table21.client.CasinoClient
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class ManageTableView(private val client: CasinoClient) {
    fun getView(onBack: () -> Unit): VBox {
        val status = AdminScreenStyle.label("Loading table status...")
        val message = AdminScreenStyle.label("")
        val refresh = AdminScreenStyle.button("Refresh")
        val open = AdminScreenStyle.button("Open Table")
        val close = AdminScreenStyle.button("Close Table")

        fun request() {
            if (!client.isConnected()) { status.text = "Not connected to the server."; return }
            client.sendCommand("GET_ADMIN_STATUS")
        }
        client.setMessageListener { line -> Platform.runLater {
            when {
                line.startsWith("ADMIN_STATUS|") -> {
                    val f = line.split('|')
                    status.text = "Table #1\nStatus: ${if (f.getOrNull(6) == "true") "OPEN" else "CLOSED"}\nRound: ${f.getOrNull(1)}\nGame phase: ${f.getOrNull(2)}\nPlayers seated: ${f.getOrNull(5)} / 4\nDealer cards: ${f.getOrNull(4)}\nPersistence issue: ${f.getOrNull(7)}"
                }
                line.startsWith("ADMIN_TABLE|") -> { message.text = "${line.substringAfter('|')}."; request() }
                line.startsWith("ERROR|") -> message.text = line.substringAfter('|')
            }
        } }
        refresh.setOnAction { request() }
        open.setOnAction { client.sendCommand("SET_TABLE_OPEN|true") }
        close.setOnAction { client.sendCommand("SET_TABLE_OPEN|false") }
        request()
        return AdminScreenStyle.root("Manage Blackjack Table", onBack, listOf(
            status, HBox(10.0, open, close, refresh), message,
            AdminScreenStyle.label("The table can only be opened or closed between rounds. Closing prevents new players from joining.")
        ))
    }
}
