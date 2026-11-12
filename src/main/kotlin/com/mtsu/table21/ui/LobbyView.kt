package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage
import java.util.Base64

class LobbyView(private val casinoClient: CasinoClient) {
    private data class LobbyTable(
        val id: String,
        val number: Int,
        val players: Int,
        val isOpen: Boolean,
        val phase: String
    )

    fun getView(userName: String, stage: Stage): VBox {
        casinoClient.setBankruptcyListener {
            javafx.application.Platform.runLater {
                casinoClient.setMessageListener(null)
                casinoClient.disconnect()
                javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION).apply {
                    initOwner(stage)
                    title = "Run complete"
                    headerText = "You went broke!"
                    contentText = "Your high score has been saved. Create a new account for a free $1,000 and try to beat the house again."
                }.showAndWait()
                stage.scene = Scene(LoginView(casinoClient).getView(stage), 800.0, 600.0)
            }
        }
        val title = Label("Welcome to Table21")
        val username = Label("Welcome, $userName")
        val chips = Label(balanceText())
        val tablesTitle = Label("Available Tables")
        val status = Label("Choose a table to join, or create a new table.")
        val tableCards = HBox(18.0).apply { alignment = Pos.CENTER_LEFT; padding = Insets(10.0) }
        val tableScroll = ScrollPane(tableCards).apply {
            isFitToHeight = true
            isPannable = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            prefViewportHeight = 230.0
            style = "-fx-background: #123D2B; -fx-background-color: #123D2B; -fx-border-color: transparent;"
        }
        val refresh = Button("Refresh Tables")
        val create = Button("+ Create New Table")
        val logout = Button("Logout")
        val leaderboard = Button("Leaderboard")

        title.font = Font.font("Arial", 32.0)
        title.textFill = Color.GOLD
        username.font = Font.font(18.0)
        username.textFill = Color.WHITE
        chips.font = Font.font(15.0)
        chips.textFill = Color.GOLD
        tablesTitle.font = Font.font("Arial", 24.0)
        tablesTitle.textFill = Color.WHITE
        status.textFill = Color.LIGHTGOLDENRODYELLOW
        status.isWrapText = true
        status.maxWidth = 650.0

        val buttonStyle = """
            -fx-background-color: #E3C46B;
            -fx-text-fill: #123D2B;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-background-radius: 8px;
            -fx-cursor: hand;
        """.trimIndent()
        listOf(refresh, create, leaderboard, logout).forEach {
            it.style = buttonStyle
            it.prefWidth = 190.0
            it.prefHeight = 40.0
        }

        // The server sends JOINED and the first STATE immediately around TABLE_JOINED.
        // Keep those messages until the blackjack view has installed its listener.
        val joinMessageLock = Any()
        val pendingJoinMessages = mutableListOf<String>()
        var waitingForTableView = false

        fun beginJoining() {
            synchronized(joinMessageLock) {
                pendingJoinMessages.clear()
                waitingForTableView = true
            }
        }

        fun enterTable(tableNumber: Int) {
            val tableView = BlackjackTableView(casinoClient, userName, tableNumber) {
                val lobby = LobbyView(casinoClient)
                stage.scene = Scene(lobby.getView(userName, stage), 900.0, 650.0)
            }
            stage.title = "Table21 - Table #$tableNumber"
            stage.scene = Scene(tableView, 900.0, 650.0)

            // The constructor above installs the table listener. Replay any join
            // handshake messages that arrived while the lobby was still visible.
            val initialMessages = synchronized(joinMessageLock) {
                waitingForTableView = false
                pendingJoinMessages.toList().also { pendingJoinMessages.clear() }
            }
            initialMessages.forEach(tableView::acceptServerMessage)
        }

        fun makeTableCard(table: LobbyTable): VBox {
            val tableName = Label("Table #${table.number}").apply {
                font = Font.font("Arial", 21.0)
                textFill = Color.GOLD
            }
            val game = Label("Blackjack · Up to 4 players").apply { textFill = Color.WHITE }
            val count = Label("Players: ${table.players} / 4").apply { textFill = Color.WHITE }
            val availability = Label(if (table.isOpen) "OPEN · ${table.phase}" else "CLOSED").apply {
                textFill = if (table.isOpen) Color.LIGHTGREEN else Color.SALMON
                font = Font.font("Arial", 12.0)
            }
            val join = Button("Join Table").apply {
                style = buttonStyle
                maxWidth = Double.MAX_VALUE
                isDisable = !table.isOpen || table.players >= 4
                setOnAction {
                    if (casinoClient.isConnected()) {
                        beginJoining()
                        status.text = "Joining Table #${table.number}..."
                        casinoClient.sendCommand("JOIN_TABLE|${table.id}")
                    } else status.text = "Connection lost. Please log in again."
                }
            }
            return VBox(10.0, tableName, game, count, availability, join).apply {
                padding = Insets(18.0)
                prefWidth = 225.0
                minWidth = 225.0
                minHeight = 190.0
                alignment = Pos.CENTER_LEFT
                style = "-fx-background-color: #0A4937; -fx-border-color: #D6B85C; -fx-border-radius: 10; -fx-background-radius: 10;"
            }
        }

        fun showTables(line: String) {
            val encoded = line.substringAfter('|')
            val entries = if (encoded.isBlank()) emptyList() else encoded.split(',').mapNotNull { token ->
                try {
                    val f = String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8).split('~')
                    if (f.size < 5) null else LobbyTable(
                        id = f[0], number = f[1].toInt(), players = f[2].toInt(),
                        isOpen = f[3].toBoolean(), phase = f[4]
                    )
                } catch (_: Exception) { null }
            }
            tableCards.children.setAll(entries.map(::makeTableCard))
            if (entries.isEmpty()) {
                tableCards.children.setAll(Label("No tables are currently available.").apply { textFill = Color.WHITE })
            }
            status.text = "${entries.size} table(s) available. Select one to join."
        }

        casinoClient.setMessageListener { line ->
            val bufferedForTable = synchronized(joinMessageLock) {
                if (waitingForTableView &&
                    (line.startsWith("JOINED|") || line.startsWith("STATE|"))) {
                    pendingJoinMessages.add(line)
                    true
                } else false
            }

            if (!bufferedForTable) Platform.runLater {
                when {
                    line.startsWith("TABLE_LIST|") -> showTables(line)
                    line.startsWith("TABLE_JOINED|") -> {
                        val fields = line.split('|')
                        enterTable(fields.getOrNull(2)?.toIntOrNull() ?: 1)
                    }
                    line.startsWith("ERROR|") -> {
                        synchronized(joinMessageLock) {
                            waitingForTableView = false
                            pendingJoinMessages.clear()
                        }
                        status.text = line.substringAfter('|')
                    }
                    line == "DISCONNECTED" -> {
                        synchronized(joinMessageLock) {
                            waitingForTableView = false
                            pendingJoinMessages.clear()
                        }
                        status.text = "Disconnected from the server. Please log in again."
                    }
                }
            }
        }

        refresh.setOnAction { casinoClient.sendCommand("GET_TABLE_LIST") }
        create.setOnAction {
            if (casinoClient.isConnected()) {
                beginJoining()
                status.text = "Creating a new table..."
                casinoClient.sendCommand("CREATE_TABLE")
            } else status.text = "Connection lost. Please log in again."
        }
        leaderboard.setOnAction {
            stage.scene = Scene(LeaderboardView(casinoClient).getView {
                stage.scene = Scene(getView(userName, stage), 800.0, 600.0)
            }, 800.0, 600.0)
        }
        logout.setOnAction {
            casinoClient.setMessageListener(null)
            casinoClient.disconnect()
            stage.scene = Scene(LoginView(CasinoClient()).getView(stage), 900.0, 650.0)
        }

        val top = HBox(25.0, username, chips).apply { alignment = Pos.CENTER }
        val actions = HBox(12.0, refresh, create, leaderboard, logout).apply { alignment = Pos.CENTER }
        val spacer1 = Region().also { VBox.setVgrow(it, Priority.ALWAYS) }
        val spacer2 = Region().also { VBox.setVgrow(it, Priority.ALWAYS) }
        val root = VBox(18.0, title, top, spacer1, tablesTitle, tableScroll, actions, status, spacer2).apply {
            padding = Insets(25.0)
            alignment = Pos.CENTER
            style = "-fx-background-color: #123D2B;"
        }
        VBox.setVgrow(tableScroll, Priority.NEVER)
        casinoClient.sendCommand("GET_TABLE_LIST")
        return root
    }

    private fun balanceText(): String = casinoClient.playerBalance?.let {
        "Balance: ${java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).format(it)}"
    } ?: "Balance available after joining"
}
