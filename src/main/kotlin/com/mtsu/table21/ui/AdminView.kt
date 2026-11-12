package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import com.mtsu.table21.ui.admin.GameStatusView
import com.mtsu.table21.ui.admin.ManageTableView
import com.mtsu.table21.ui.admin.PlayerView
import com.mtsu.table21.ui.admin.ServerInfoView
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage

class AdminView(private val casinoClient: CasinoClient) {
    fun getView(stage: Stage): BorderPane {
        val root = BorderPane().apply {
            padding = Insets(22.0)
            style = "-fx-background-color: #063D2B;"
        }
        val title = Label("♠ Table21  |  ADMIN PANEL").apply {
            font = Font.font("Arial", 27.0)
            textFill = Color.web("#F4D27A")
        }
        val welcome = Label("Administrator: ${casinoClient.authenticatedUsername ?: "Unknown"}").apply {
            textFill = Color.WHITE
            font = Font.font(14.0)
        }
        val header = VBox(7.0, title, welcome).apply { padding = Insets(0.0, 0.0, 18.0, 0.0) }
        root.top = header

        val content = VBox(15.0)
        content.alignment = Pos.CENTER
        root.center = content

        fun showScreen(screen: VBox) {
            content.children.setAll(screen)
        }

        fun showHome() {
            content.children.clear()
            val heading = Label("Administration Tools").apply {
                textFill = Color.WHITE
                font = Font.font("Arial", 22.0)
            }
            val description = Label("Monitor the live blackjack table, manage seated players, and check server information.").apply {
                textFill = Color.web("#D8E7DC")
                isWrapText = true
                maxWidth = 560.0
            }
            val grid = GridPane().apply { hgap = 18.0; vgap = 18.0; maxWidth = 620.0 }
            fun navButton(text: String, detail: String, action: () -> Unit): VBox {
                val button = Button(text).apply {
                    maxWidth = Double.MAX_VALUE
                    prefHeight = 42.0
                    style = "-fx-background-color: #E6C66C; -fx-text-fill: #063D2B; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 7; -fx-cursor: hand;"
                    setOnAction { action() }
                }
                val textLabel = Label(detail).apply {
                    textFill = Color.web("#D8E7DC")
                    isWrapText = true
                    maxWidth = Double.MAX_VALUE
                }
                return VBox(10.0, button, textLabel).apply {
                    padding = Insets(18.0)
                    minWidth = 270.0
                    minHeight = 115.0
                    style = "-fx-background-color: #0A4937; -fx-border-color: #168A70; -fx-border-radius: 9; -fx-background-radius: 9;"
                    GridPane.setHgrow(this, Priority.ALWAYS)
                }
            }
            grid.add(navButton("Manage Tables", "Open or close Table #1 between rounds.") {
                showScreen(ManageTableView(casinoClient).getView(::showHome))
            }, 0, 0)
            grid.add(navButton("View Players", "See player names, seats, balances, and table status.") {
                showScreen(PlayerView(casinoClient).getView(::showHome))
            }, 1, 0)
            grid.add(navButton("Game Status", "Monitor the current round, phase, dealer, and player count.") {
                showScreen(GameStatusView(casinoClient).getView(::showHome))
            }, 0, 1)
            grid.add(navButton("Server Information", "Check uptime, active connections, port, and house balance.") {
                showScreen(ServerInfoView(casinoClient).getView(::showHome))
            }, 1, 1)
            content.children.addAll(heading, description, grid)
        }
        showHome()

        val logout = Button("⇥  Logout").apply {
            prefWidth = 140.0
            prefHeight = 40.0
            style = "-fx-background-color: transparent; -fx-border-color: #F4D27A; -fx-border-width: 2; -fx-border-radius: 8; -fx-text-fill: #F4D27A; -fx-font-size: 14px; -fx-cursor: hand;"
            setOnAction {
                casinoClient.setMessageListener(null)
                casinoClient.disconnect()
                stage.scene = Scene(LoginView(CasinoClient()).getView(stage), 800.0, 600.0)
            }
        }
        root.bottom = HBox(logout).apply {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(15.0, 0.0, 0.0, 0.0)
        }
        if (!casinoClient.isAdmin) {
            root.center = Label("Administrator access required.").apply { textFill = Color.WHITE }
        }
        return root
    }
}
