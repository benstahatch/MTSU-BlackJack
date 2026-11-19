package com.mtsu.table21.ui.admin

import com.mtsu.table21.client.CasinoClient
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import java.util.Base64

class GameStatusView(private val client: CasinoClient) {
    fun getView(onBack: () -> Unit): VBox {
        val tableList = VBox(12.0)
        val message = AdminScreenStyle.label("Loading all tables...")
        val refresh = AdminScreenStyle.button("Refresh All Tables")

        fun renderTables(line: String) {
            val encoded = line.substringAfter('|')
            val rows = if (encoded.isBlank()) emptyList() else encoded.split(',').mapNotNull { token ->
                try {
                    val fields = String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8).split('~')
                    if (fields.size < 9) null else fields
                } catch (_: Exception) { null }
            }
            tableList.children.clear()
            if (rows.isEmpty()) {
                message.text = "No tables have been created."
                return
            }
            message.text = "${rows.size} table(s) currently managed by the server."
            rows.forEach { f ->
                val number = f[1]
                val round = f[2]
                val phase = f[3]
                val currentSeat = f[4].toIntOrNull()?.let { if (it < 0) "None" else "Seat ${it + 1}" } ?: "Unknown"
                val dealerCards = f[5]
                val players = f[6]
                val open = f[7].toBoolean()
                val persistence = f[8].toBoolean()
                val card = VBox(7.0).apply {
                    padding = Insets(16.0)
                    style = "-fx-background-color: #0A4937; -fx-border-color: #168A70; -fx-border-radius: 9; -fx-background-radius: 9;"
                    children.add(Label("Table #$number").apply {
                        textFill = Color.web("#F4D27A")
                        style = "-fx-font-size: 19px; -fx-font-weight: bold;"
                    })
                    children.add(Label("Status: ${if (open) "OPEN" else "CLOSED"}").apply {
                        textFill = if (open) Color.LIGHTGREEN else Color.SALMON
                    })
                    children.add(AdminScreenStyle.label("Players seated: $players / 4"))
                    children.add(AdminScreenStyle.label("Round: $round    Phase: $phase"))
                    children.add(AdminScreenStyle.label("Current player: $currentSeat"))
                    children.add(AdminScreenStyle.label("Dealer cards: $dealerCards"))
                    children.add(AdminScreenStyle.label("Wallet persistence error: $persistence"))
                }
                tableList.children.add(card)
            }
        }

        client.setMessageListener { line -> Platform.runLater {
            when {
                line.startsWith("ADMIN_TABLES|") -> renderTables(line)
                line.startsWith("ERROR|") -> message.text = line.substringAfter('|')
            }
        } }
        refresh.setOnAction { client.sendCommand("GET_ADMIN_TABLES") }
        client.sendCommand("GET_ADMIN_TABLES")

        val scroll = ScrollPane(tableList).apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            prefViewportHeight = 420.0
            style = "-fx-background: #0A4937; -fx-background-color: #0A4937; -fx-border-color: transparent;"
        }
        return AdminScreenStyle.root("Game Status — All Tables", {
            client.setMessageListener(null)
            onBack()
        }, listOf(
            message, HBox(10.0, refresh), scroll,
            AdminScreenStyle.label("Each card shows the live server-side state for an independent blackjack table.")
        ))
    }
}
