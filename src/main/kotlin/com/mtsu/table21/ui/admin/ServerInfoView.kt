package com.mtsu.table21.ui.admin

import com.mtsu.table21.client.CasinoClient
import javafx.application.Platform
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import java.text.NumberFormat
import java.util.Locale

class ServerInfoView(private val client: CasinoClient) {
    fun getView(onBack: () -> Unit): VBox {
        val server = AdminScreenStyle.label("Loading server information...")
        val house = AdminScreenStyle.label("House balance: loading...")
        val message = AdminScreenStyle.label("")
        val refresh = AdminScreenStyle.button("Refresh Information")
        client.setMessageListener { line -> Platform.runLater {
            when {
                line.startsWith("ADMIN_SERVER|") -> {
                    val f = line.split('|')
                    val millis = f.getOrNull(1)?.toLongOrNull() ?: 0L
                    val seconds = millis / 1000
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    val remaining = seconds % 60
                    server.text = "Server status: Connected\nUptime: ${hours}h ${minutes}m ${remaining}s\nActive client connections: ${f.getOrNull(2)}\nTCP port: ${f.getOrNull(3)}"
                }
                line.startsWith("HOUSE_BALANCE|") -> {
                    val value = line.substringAfter('|').toDoubleOrNull()
                    house.text = value?.let { "House balance: ${NumberFormat.getCurrencyInstance(Locale.US).format(it)}" } ?: "House balance unavailable"
                }
                line.startsWith("ERROR|") -> message.text = line.substringAfter('|')
            }
        } }
        refresh.setOnAction { client.sendCommand("GET_ADMIN_SERVER"); client.sendCommand("GET_HOUSE_BALANCE") }
        refresh.fire()
        return AdminScreenStyle.root("Server Information", onBack, listOf(
            server, house, HBox(10.0, refresh), message,
            AdminScreenStyle.label("Connection count and uptime are process-level server metrics. Database credentials are not displayed here.")
        ))
    }
}
