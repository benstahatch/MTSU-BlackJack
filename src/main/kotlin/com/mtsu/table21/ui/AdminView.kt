package com.mtsu.table21.ui

import javafx.stage.Stage
import com.mtsu.table21.client.CasinoClient
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
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.layout.Region
import javafx.scene.shape.Circle

class AdminView (private val casinoClient: CasinoClient){
    fun getView(stage: Stage): BorderPane {
        //creates a border like sence with four thing
        val root = BorderPane()
        root.padding = Insets(25.0)
        root.style="-fx-background-color: #063D2B;"

        //create val for admin screens
        val title = Label("♠ Table21")
        title.font = Font.font(30.0)
        title.textFill = Color.web("#F4D27A")

        val adminPanel = Label("ADMIN PANEL")
        adminPanel.font = Font.font(30.0)
        adminPanel.textFill = Color.WHITE

        val titleArea = HBox(15.0, title, adminPanel)

        val tableButton = Button("Manage Tables")
        val playerButton = Button("View Players")
        val statusButton = Button("View Game Status")
        val serverButton = Button("View Server Info")
        val logoutButton = Button("⇥  Logout")

        //button width
        tableButton.prefWidth=250.0
        playerButton.prefWidth=250.0
        statusButton.prefWidth=250.0
        serverButton.prefWidth=250.0
        logoutButton.prefWidth=150.0
        logoutButton.prefHeight = 42.0
        logoutButton.style = """
            -fx-background-color: transparent;
            -fx-border-color: #F4D27A;
            -fx-border-width: 2;
            -fx-border-radius: 8;
            -fx-text-fill: #F4D27A;
            -fx-font-size: 15px;
            -fx-cursor: hand;
        """

        //sever status
        val statusCircle = Circle(6.0, Color.LIGHTGREEN)
        val serverStatus=Label("Server Connected")
        serverStatus.textFill = Color.LIGHTGREEN
        serverStatus.font = Font.font(14.0)

        val serverText = HBox(8.0, statusCircle, serverStatus)
        serverText.padding = Insets(10.0, 16.0, 10.0, 16.0)
        serverText.style = """
            -fx-background-color: transparent;
            -fx-border-color: #00C781;
            -fx-border-width: 2;
            -fx-border-radius: 25;
            -fx-background-radius: 25;
        """
        serverText.alignment = Pos.CENTER

        //header of the screen
        val header=BorderPane()
        header.left = titleArea
        header.right = serverText
        header.padding = Insets(0.0, 0.0, 15.0, 0.0)
        root.top = header

        // Gold line under the header
        val goldLine = Region()
        goldLine.prefHeight = 2.0
        goldLine.maxHeight = 2.0
        goldLine.style = "-fx-background-color: #F4D27A;"

        //wecome message
        val welcome = Label("Welcome, admin ")
        welcome.font = Font.font(15.0)
        welcome.textFill = Color.WHITE

        //divide the border into four section as cards
        val grid = GridPane()
        grid.hgap = 20.0
        grid.vgap = 20.0
        grid.padding = Insets(20.0, 0.0, 20.0, 0.0)

        //the differect cards
        val tableCard = createCard(
            "Manage Table",
            "View, create, open, and close blackjack tables.",
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
            "Check server connections, uptime, and system logs.",
            serverButton
        )

        grid.add(tableCard, 0,0 )
        grid.add(playerCard, 1 ,0)
        grid.add(statusCard, 0 ,1)
        grid.add(serverCard, 1 ,1)

        // Give both columns equal width
        (0..1).forEach { _ ->
            val column = ColumnConstraints()
            column.hgrow = Priority.ALWAYS
            column.percentWidth = 50.0
            grid.columnConstraints.add(column)
        }

        root.center = VBox(10.0, goldLine, welcome, grid)

        //logout action
        logoutButton.setOnAction{
            casinoClient.disconnect()
            val login=LoginView(casinoClient)
            val scene = Scene(login.getView(stage), 800.0, 600.0)
            stage.scene = scene
        }
        val bottom = HBox(logoutButton)
        bottom.alignment = Pos.CENTER_RIGHT
        bottom.padding = Insets(0.0, 0.0, 0.0, 0.0)

        root.bottom = bottom
        return root
    }

    //creates the card funtion for different cards
    private fun createCard(title: String, text: String, button: Button): VBox {
        val titles = Label(title)
        titles.font = Font.font(21.0)
        titles.textFill = Color.WHITE
        titles.alignment = Pos.CENTER
        titles.maxWidth = Double.MAX_VALUE

        val details = Label(text)
        details.font = Font.font(14.0)
        details.textFill = Color.web("#D8E7DC")
        details.isWrapText = true
        details.alignment = Pos.CENTER_LEFT
        details.maxWidth = Double.MAX_VALUE

        button.maxWidth = Double.MAX_VALUE
        button.style = """
            -fx-background-color: #E6C66C;
            -fx-text-fill: #063D2B;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-background-radius: 7;
            -fx-cursor: hand;
        """
        val card = VBox(12.0,
            titles,
            details,
            button)
        card.padding = Insets(20.0)
        card.alignment = Pos.CENTER_LEFT
        card.minHeight = 175.0
        card.maxWidth = Double.MAX_VALUE
        card.style = """
            -fx-background-color: #0A4937;
            -fx-border-color: #168A70;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
        """

        GridPane.setHgrow(card, Priority.ALWAYS)
        GridPane.setVgrow(card, Priority.ALWAYS)

        return card
    }
}