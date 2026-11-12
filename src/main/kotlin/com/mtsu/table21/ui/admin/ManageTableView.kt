package com.mtsu.table21.ui.admin

import com.mtsu.table21.client.CasinoClient
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import java.util.Base64

/** Admin screen showing every table managed by the server. */
class ManageTableView(private val client: CasinoClient) {

    private data class TableInfo(
        val id: String,
        val number: String,
        val round: String,
        val phase: String,
        val currentSeat: String,
        val dealerCards: String,
        val players: String,
        val isOpen: Boolean,
        val persistenceIssue: Boolean
    )

    fun getView(onBack: () -> Unit): VBox {
        val summary = AdminScreenStyle.label("Loading tables...")
        val message = AdminScreenStyle.label("")
        val refresh = AdminScreenStyle.button("Refresh All Tables")
        val tableList = VBox(12.0).apply {
            padding = Insets(4.0, 2.0, 8.0, 2.0)
            isFillWidth = true
        }

        fun requestTables() {
            if (!client.isConnected()) {
                summary.text = "Not connected to the server."
                tableList.children.clear()
                return
            }
            summary.text = "Loading table information..."
            client.sendCommand("GET_ADMIN_TABLES")
        }

        fun tableCard(table: TableInfo): VBox {
            val title = Label("Table #${table.number}  (ID: ${table.id})").apply {
                font = Font.font("Arial", 19.0)
                textFill = Color.web("#F4D27A")
            }
            val state = Label(if (table.isOpen) "OPEN" else "CLOSED").apply {
                font = Font.font("Arial", 13.0)
                textFill = if (table.isOpen) Color.LIGHTGREEN else Color.SALMON
                style = "-fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-border-color: ${if (table.isOpen) "#75D69A" else "#E58A80"}; -fx-border-radius: 12; -fx-background-radius: 12;"
            }
            val heading = HBox(12.0, title, state).apply {
                alignment = Pos.CENTER_LEFT
            }

            val currentSeatText = if (table.currentSeat == "-1") "None" else table.currentSeat
            val details = Label(
                "Round: ${table.round}     Phase: ${table.phase}\n" +
                    "Players seated: ${table.players} / 4     Dealer cards: ${table.dealerCards}\n" +
                    "Current seat: $currentSeatText     Persistence issue: ${if (table.persistenceIssue) "Yes" else "No"}"
            ).apply {
                textFill = Color.WHITE
                font = Font.font("Arial", 13.0)
                isWrapText = true
                lineSpacing = 4.0
            }

            val openButton = AdminScreenStyle.button("Open Table").apply {
                isDisable = table.isOpen
                setOnAction {
                    client.sendCommand("SET_TABLE_OPEN|${table.id}|true")
                }
            }
            val closeButton = AdminScreenStyle.button("Close Table").apply {
                isDisable = !table.isOpen
                setOnAction {
                    client.sendCommand("SET_TABLE_OPEN|${table.id}|false")
                }
            }
            val actions = HBox(10.0, openButton, closeButton).apply {
                alignment = Pos.CENTER_LEFT
            }

            return VBox(10.0, heading, details, actions).apply {
                padding = Insets(16.0)
                maxWidth = Double.MAX_VALUE
                style = "-fx-background-color: #063D2B; -fx-border-color: #168A70; -fx-border-radius: 9; -fx-background-radius: 9;"
                HBox.setHgrow(this, Priority.ALWAYS)
            }
        }

        fun decodeTable(encoded: String): TableInfo? {
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
                val fields = decoded.split('~')
                if (fields.size < 9) return null
                TableInfo(
                    id = fields[0],
                    number = fields[1],
                    round = fields[2],
                    phase = fields[3],
                    currentSeat = fields[4],
                    dealerCards = fields[5],
                    players = fields[6],
                    isOpen = fields[7].equals("true", ignoreCase = true),
                    persistenceIssue = fields[8].equals("true", ignoreCase = true)
                )
            } catch (_: Exception) {
                null
            }
        }

        client.setMessageListener { line ->
            Platform.runLater {
                when {
                    line.startsWith("ADMIN_TABLES|") -> {
                        val encodedRecords = line.substringAfter('|')
                        val tables = if (encodedRecords.isBlank()) emptyList() else
                            encodedRecords.split(',').mapNotNull(::decodeTable).sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }

                        tableList.children.setAll(tables.map(::tableCard))
                        summary.text = when (tables.size) {
                            0 -> "No tables are currently available."
                            1 -> "Showing 1 table."
                            else -> "Showing all ${tables.size} tables."
                        }
                    }
                    line.startsWith("ADMIN_TABLE|") -> {
                        message.text = "Table updated: ${line.substringAfter('|')}"
                        requestTables()
                    }
                    line.startsWith("ERROR|") -> {
                        message.text = line.substringAfter('|')
                    }
                    line == "DISCONNECTED" -> {
                        summary.text = "Disconnected from the server."
                    }
                }
            }
        }

        refresh.setOnAction { requestTables() }

        val scrollPane = ScrollPane(tableList).apply {
            isFitToWidth = true
            isFitToHeight = false
            prefViewportHeight = 430.0
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            style = "-fx-background: #0A4937; -fx-background-color: #0A4937; -fx-border-color: transparent;"
        }

        requestTables()
        return AdminScreenStyle.root(
            "Manage Blackjack Tables",
            onBack,
            listOf(summary, refresh, message, scrollPane as Node,
                AdminScreenStyle.label("Each card shows the status of one server table. Tables can only be opened or closed between rounds."))
        )
    }
}
