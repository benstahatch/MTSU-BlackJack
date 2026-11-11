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
import java.util.Base64

class LobbyView(private val casinoClient: CasinoClient) {
    fun getView(userName: String, stage: Stage): VBox {
        val title = Label("Welcome to Table21")
        val username = Label("Welcome, $userName")
        val chips = Label("Virtual starting balance: $1,000.00")
        val tables = Label("Available Tables")
        val tableDescription = Label("Table #1  •  Blackjack  •  Up to 4 players")
        val status = Label("Choose Join Table or Create Table to sit down.")
        val join = Button("Join Table")
        val create = Button("Create Table")
        val logout = Button("Logout")

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
        listOf(join, create, logout).forEach {
            it.style = buttonStyle
            it.prefWidth = 200.0
            it.prefHeight = 40.0
        }

        fun enterTable() {
            if (!casinoClient.isConnected() && !casinoClient.connect()) {
                status.text = "Cannot connect to the server. Check the server computer and Wi-Fi."
                return
            }
            val tableView = BlackjackTableView(casinoClient, userName) {
                val lobby = LobbyView(casinoClient)
                stage.scene = Scene(lobby.getView(userName, stage), 800.0, 600.0)
            }
            stage.title = "Table21 - Blackjack"
            stage.scene = Scene(tableView, 800.0, 600.0)
            val encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(userName.toByteArray(Charsets.UTF_8))
            casinoClient.sendCommand("JOIN_TABLE|$encodedName")
        }

        join.setOnAction { enterTable() }
        create.setOnAction { enterTable() }
        logout.setOnAction {
            casinoClient.disconnect()
            stage.scene = Scene(LoginView(CasinoClient()).getView(stage), 800.0, 600.0)
        }

        val top = HBox(25.0, username, chips).apply { alignment = Pos.CENTER }
        val buttons = VBox(12.0, join, create, logout).apply { alignment = Pos.CENTER }
        val spacer1 = Region().also { VBox.setVgrow(it, Priority.ALWAYS) }
        val spacer2 = Region().also { VBox.setVgrow(it, Priority.ALWAYS) }
        return VBox(18.0, title, top, spacer1, tables, tableDescription, buttons, status, spacer2).apply {
            padding = Insets(25.0)
            alignment = Pos.CENTER
            style = "-fx-background-color: #123D2B;"
        }
    }
}
