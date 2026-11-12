
package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import com.mtsu.table21.ui.admin.GameStatusView
import com.mtsu.table21.ui.admin.ManageTableView
import com.mtsu.table21.ui.admin.PlayerView
import com.mtsu.table21.ui.admin.ServerInfoView

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Font
import javafx.stage.Stage

class AdminView(private val casinoClient: CasinoClient) {

    fun getView(stage: Stage): BorderPane {

        val root = BorderPane()
        root.padding = Insets(15.0)
        root.style = "-fx-background-color: #063D2B;"

        // ------------------------------------------------
        // HEADER
        // ------------------------------------------------

        val title = Label("♠ Table21").apply {
            font = Font.font(30.0)
            textFill = Color.web("#F4D27A")
        }

        val adminPanel = Label("ADMIN PANEL").apply {
            font = Font.font(30.0)
            textFill = Color.WHITE
        }

        val titleArea = HBox(15.0, title, adminPanel).apply {
            alignment = Pos.CENTER_LEFT
        }

        val statusCircle = Circle(6.0, Color.LIGHTGREEN)

        val serverStatus = Label("Server Connected").apply {
            textFill = Color.LIGHTGREEN
            font = Font.font(14.0)
        }

        val serverText = HBox(8.0, statusCircle, serverStatus).apply {
            padding = Insets(10.0, 16.0, 10.0, 16.0)
            alignment = Pos.CENTER
            style = """
                -fx-background-color: transparent;
                -fx-border-color: #00C781;
                -fx-border-width: 2;
                -fx-border-radius: 25;
                -fx-background-radius: 25;
            """
        }

        val header = BorderPane().apply {
            left = titleArea
            right = serverText
            padding = Insets(0.0, 0.0, 15.0, 0.0)
        }

        root.top = header

        // ------------------------------------------------
        // DASHBOARD CONTENT
        // ------------------------------------------------

        val content = VBox(8.0)

        var returnToHome: () -> Unit = {}

        fun showScreen(screen: VBox) {
            content.children.setAll(screen)
        }

        val goldLine = Region().apply {
            prefHeight = 2.0
            maxHeight = 2.0
            style = "-fx-background-color: #F4D27A;"
        }

        val welcome = Label(
            "Welcome, ${casinoClient.authenticatedUsername ?: "Admin"}"
        ).apply {
            font = Font.font(17.0)
            textFill = Color.WHITE
        }

        val houseBalance = Label("House Balance: loading...").apply {
            textFill = Color.GOLD
            font = Font.font(24.0)
        }

        val refreshBalance = Button("Refresh Balance")

        // ------------------------------------------------
        // HOUSE BALANCE
        // ------------------------------------------------

        fun requestBalance() {
            Thread({
                val connected = casinoClient.isConnected() ||
                        casinoClient.connect()

                val sent = connected &&
                        casinoClient.sendCommand("GET_HOUSE_BALANCE") == "SENT"

                Platform.runLater {
                    serverStatus.text =
                        if (connected) "Server Connected"
                        else "Server Disconnected"

                    statusCircle.fill =
                        if (connected) Color.LIGHTGREEN
                        else Color.RED

                    if (!sent) {
                        houseBalance.text = "House Balance unavailable"
                    }
                }
            }, "Table21-house-balance").apply {
                isDaemon = true
                start()
            }
        }

        refreshBalance.setOnAction {
            requestBalance()
        }

        // ------------------------------------------------
        // ADMIN NAVIGATION BUTTONS
        // ------------------------------------------------

        val tableButton = Button("Manage Tables")
        val playerButton = Button("View Players")
        val statusButton = Button("View Game Status")
        val serverButton = Button("View Server Info")

        // Manage Tables
        tableButton.setOnAction {
            showScreen(
                ManageTableView(casinoClient).getView({ returnToHome() })
            )
        }

        // View Players
        playerButton.setOnAction {
            showScreen(
                PlayerView(casinoClient).getView({ returnToHome() })
            )
        }

        // Game Status
        statusButton.setOnAction {
            showScreen(
                GameStatusView(casinoClient).getView({ returnToHome() })
            )
        }

        // Server Information
        serverButton.setOnAction {
            showScreen(
                ServerInfoView(casinoClient).getView({ returnToHome() })
            )
        }

        // ------------------------------------------------
        // DASHBOARD CARDS
        // ------------------------------------------------

        val grid = GridPane().apply {
            hgap = 12.0
            vgap = 12.0
            padding = Insets(10.0, 0.0, 10.0, 0.0)

            repeat(2) {
                columnConstraints.add(
                    ColumnConstraints().apply {
                        percentWidth = 50.0
                        hgrow = Priority.ALWAYS
                    }
                )
            }
        }

        val tableCard = createCard(
            "Manage Tables",
            "View, open, and close blackjack tables.",
            tableButton
        )

        val playerCard = createCard(
            "View Players",
            "See connected players and their table assignments.",
            playerButton
        )

        val statusCard = createCard(
            "Game Status",
            "Monitor active games and current rounds.",
            statusButton
        )

        val serverCard = createCard(
            "Server Information",
            "Check server connections, uptime, and house balance.",
            serverButton
        )

        grid.add(tableCard, 0, 0)
        grid.add(playerCard, 1, 0)
        grid.add(statusCard, 0, 1)
        grid.add(serverCard, 1, 1)

        // ------------------------------------------------
        // RETURN TO DASHBOARD
        // ------------------------------------------------

        fun showHome() {
            content.children.setAll(
                goldLine,
                welcome,
                houseBalance,
                refreshBalance,
                grid
            )

            casinoClient.setMessageListener { message ->
                Platform.runLater {
                    when {
                        message.startsWith("HOUSE_BALANCE|") -> {
                            val balance = message.substringAfter("|").toBigDecimalOrNull()

                            houseBalance.text = balance?.let {
                                "House Balance: ${
                                    java.text.NumberFormat
                                        .getCurrencyInstance(java.util.Locale.US)
                                        .format(it)
                                }"
                            } ?: "House Balance unavailable"
                        }

                        message.startsWith("ERROR|") -> {
                            houseBalance.text = "House Balance unavailable"
                        }

                        message == "DISCONNECTED" -> {
                            serverStatus.text = "Server Disconnected"
                            statusCircle.fill = Color.RED
                        }
                    }
                }
            }

            requestBalance()
        }

// Set the callback outside showHome()
        returnToHome = { showHome() }

        val scrollPane = javafx.scene.control.ScrollPane(content).apply {
            isFitToWidth = true
            isFitToHeight = false
            hbarPolicy = javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
            isPannable = true
            style = """
        -fx-background: #063D2B;
        -fx-background-color: #063D2B;
        -fx-border-color: transparent;
    """
        }

        root.center = scrollPane

        // ------------------------------------------------
        // LOGOUT
        // ------------------------------------------------

        val logoutButton = Button("⇥  Logout").apply {
            prefWidth = 150.0
            prefHeight = 42.0

            style = """
                -fx-background-color: transparent;
                -fx-border-color: #F4D27A;
                -fx-border-width: 2;
                -fx-border-radius: 8;
                -fx-text-fill: #F4D27A;
                -fx-font-size: 15px;
                -fx-cursor: hand;
            """

            setOnAction {
                casinoClient.setMessageListener(null)
                casinoClient.disconnect()

                stage.scene = Scene(
                    LoginView(CasinoClient()).getView(stage),
                    800.0,
                    600.0
                )
            }
        }

        root.bottom = HBox(logoutButton).apply {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(15.0, 0.0, 0.0, 0.0)
        }

        // Show dashboard initially
        showHome()

        return root
    }

    // ------------------------------------------------
    // CARD CREATION
    // ------------------------------------------------

    private fun createCard(
        title: String,
        text: String,
        button: Button
    ): VBox {

        val titleLabel = Label(title).apply {
            font = Font.font(21.0)
            textFill = Color.WHITE
            alignment = Pos.CENTER
            maxWidth = Double.MAX_VALUE
        }

        val details = Label(text).apply {
            font = Font.font(14.0)
            textFill = Color.web("#D8E7DC")
            isWrapText = true
            alignment = Pos.CENTER_LEFT
            maxWidth = Double.MAX_VALUE
        }

        button.maxWidth = Double.MAX_VALUE
        button.prefHeight = 40.0

        button.style = """
            -fx-background-color: #E6C66C;
            -fx-text-fill: #063D2B;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-background-radius: 7;
            -fx-cursor: hand;
        """

        return VBox(12.0, titleLabel, details, button).apply {
            padding = Insets(20.0)
            alignment = Pos.CENTER_LEFT
            minHeight = 135.0
            maxWidth = Double.MAX_VALUE

            style = """
                -fx-background-color: #0A4937;
                -fx-border-color: #168A70;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
            """

            GridPane.setHgrow(this, Priority.ALWAYS)
            GridPane.setVgrow(this, Priority.ALWAYS)
        }
    }
}
