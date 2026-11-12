package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage

class LobbyView(private val casinoClient: CasinoClient) {
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
        val chips = Label(casinoClient.playerBalance?.let { "Balance: ${java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).format(it)}" }
            ?: "Balance available after joining")
        val tables = Label("Available Tables")
        val tableDescription = Label("Table #1  •  Blackjack  •  Up to 4 players")
        val status = Label("Choose Join Table or Create Table to sit down.")
        val join = Button("Join Table")
        val create = Button("Create Table")
        val logout = Button("Logout")
        val leaderboard = Button("Leaderboard")

        title.font = Font.font("Arial", 32.0)
        title.textFill = Color.GOLD
        username.font = Font.font(18.0)
        username.textFill = Color.WHITE
        chips.font = Font.font(15.0)
        chips.textFill = Color.GOLD
        tables.font = Font.font(24.0)
        tables.textFill = Color.WHITE
        tableDescription.textFill = Color.WHITE
        status.textFill = Color.LIGHTGOLDENRODYELLOW
        status.isWrapText = true
        status.maxWidth = 560.0

        val buttonStyle = """
            -fx-background-color: #E3C46B;
            -fx-text-fill: #123D2B;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 8px;
            -fx-cursor: hand;
        """.trimIndent()
        listOf(join, create, leaderboard, logout).forEach {
            it.style = buttonStyle
            it.prefWidth = 200.0
            it.prefHeight = 40.0
        }

        fun enterTable() {
            if (!casinoClient.isConnected() || casinoClient.authenticatedUserId == null) {
                status.text = "Connection lost. Log out and log in again."
                return
            }
            val tableView = BlackjackTableView(casinoClient, userName) {
                val lobby = LobbyView(casinoClient)
                stage.scene = Scene(lobby.getView(userName, stage), 800.0, 600.0)
            }
            stage.title = "Table21 - Blackjack"
            stage.scene = Scene(tableView, 800.0, 600.0)
            casinoClient.sendCommand("JOIN_TABLE")
        }

        join.setOnAction { enterTable() }
        create.setOnAction { enterTable() }
        leaderboard.setOnAction {
            stage.scene = Scene(LeaderboardView(casinoClient).getView {
                stage.scene = Scene(getView(userName, stage), 800.0, 600.0)
            }, 800.0, 600.0)
        }
        logout.setOnAction {
            casinoClient.disconnect()
            stage.scene = Scene(LoginView(CasinoClient()).getView(stage), 800.0, 600.0)
        }

        val top = HBox(25.0, username, chips).apply { alignment = Pos.CENTER }
        val buttons = VBox(12.0, join, create, leaderboard, logout).apply { alignment = Pos.CENTER }
        val spacer1 = Region().also { VBox.setVgrow(it, Priority.ALWAYS) }
        val spacer2 = Region().also { VBox.setVgrow(it, Priority.ALWAYS) }
        return VBox(18.0, title, top, spacer1, tables, tableDescription, buttons, status, spacer2).apply {
            padding = Insets(25.0)
            alignment = Pos.CENTER
            style = "-fx-background-color: #123D2B;"
        }
    }
}
