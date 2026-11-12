package com.mtsu.table21.ui.admin

import com.mtsu.table21.client.CasinoClient
import javafx.application.Platform
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class GameStatusView(private val client: CasinoClient) {
    fun getView(onBack: () -> Unit): VBox {
        val details = AdminScreenStyle.label("Loading game status...")
        val refresh = AdminScreenStyle.button("Refresh Status")
        client.setMessageListener { line -> Platform.runLater {
            when {
                line.startsWith("ADMIN_STATUS|") -> {
                    val f = line.split('|')
                    details.text = "Round: ${f.getOrNull(1)}\nPhase: ${f.getOrNull(2)}\nCurrent player seat: ${f.getOrNull(3)?.toIntOrNull()?.let { if (it < 0) "None" else it + 1 }}\nDealer cards: ${f.getOrNull(4)}\nPlayers seated: ${f.getOrNull(5)} / 4\nTable: ${if (f.getOrNull(6) == "true") "Open" else "Closed"}\nWallet persistence error: ${f.getOrNull(7)}"
                }
                line.startsWith("ERROR|") -> details.text = line.substringAfter('|')
            }
        } }
        refresh.setOnAction { client.sendCommand("GET_ADMIN_STATUS") }
        client.sendCommand("GET_ADMIN_STATUS")
        return AdminScreenStyle.root("Current Game Status", onBack, listOf(
            details, HBox(10.0, refresh),
            AdminScreenStyle.label("This panel reports the live state maintained by the server, not a locally estimated game state.")
        ))
    }
}
