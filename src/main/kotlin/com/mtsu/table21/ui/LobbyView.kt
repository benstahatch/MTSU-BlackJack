package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import com.mtsu.table21.game.BlackjackTable
import com.mtsu.table21.model.Wallet
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.Region
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.Scene
import javafx.stage.Stage

class LobbyView(
    private val casinoClient: CasinoClient
) {
    fun getView(userName:String,stage: Stage): VBox {
        val table = Label("Welcome to Table21")
        val username = Label("Welcome, $userName")
        //need to link chips into player system
        val chips = Label("Chips: 1000")
        val tables = Label("Available Tables")
        val joinTables = Button("Join Tables")
        val createTables = Button("Create Tables")
        val logout = Button("Logout")

        table.font = Font.font("Arial",32.0)
        table.textFill = Color.GOLD
        username.font = Font.font(18.0)
        username.textFill = Color.WHITE
        chips.font = Font.font(18.0)
        chips.textFill = Color.GOLD

        //add a box with it being near the top
        val intoTop=HBox(
            30.0,
            username,
            chips
        )
        intoTop.alignment = Pos.CENTER

        tables.font = Font.font(24.0)
        tables.textFill = Color.WHITE
        tables.alignment = Pos.CENTER

        // Style the buttons
        val buttonStyle = """
        -fx-background-color: #E3C46B;
        -fx-text-fill: #123D2B;
        -fx-font-size: 16px;
        -fx-font-weight: bold;
        -fx-background-radius: 8px;
        -fx-cursor: hand;
        """

        joinTables.style = buttonStyle
        createTables.style = buttonStyle
        logout.style = buttonStyle

        joinTables.prefWidth = 200.0
        createTables.prefWidth = 200.0
        logout.prefWidth = 200.0

        joinTables.prefHeight = 40.0
        createTables.prefHeight = 40.0
        logout.prefHeight = 40.0

        createTables.setOnAction {

            // Create the blackjack game backend
            val blackjackTable = BlackjackTable(userName, 6)

            // Create the player's wallet
            // Default starting balance is 1000 chips
            val wallet = Wallet()

            // Create the blackjack table UI
            val blackjackTableView = BlackjackTableView(
                blackjackTable,
                wallet
            ) {
                // Disconnect the current client
                casinoClient.disconnect()

                // Create a new client
                val newClient = CasinoClient()

                // Return to login screen
                val login = LoginView(newClient)

                val loginScene = Scene(
                    login.getView(stage),
                    800.0,
                    600.0
                )

                stage.title = "Welcome to the casino"
                stage.scene = loginScene
            }

            // Update the stage
            val scene = Scene(
                blackjackTableView,
                800.0,
                600.0
            )

            stage.title = "Table21 - Blackjack"
            stage.scene = scene
        }

        //logout button action
        logout.setOnAction{

            //disconnect current client from server
            casinoClient.disconnect()

            //create a new client for login
            val newClient=CasinoClient()

            val login=LoginView(newClient)
            val scene = Scene(login.getView(stage), 800.0, 600.0)
            stage.scene = scene
        }

        //bottom half of the screen
        val bottom=VBox(
            12.0,
            joinTables,
            createTables,
            logout
        )
        bottom.alignment = Pos.CENTER

        val topSpacer =Region()
        val bottomSpacer=Region()
        VBox.setVgrow(topSpacer , Priority.ALWAYS)
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS)

        val layout = VBox(
            20.0,
            table,
            intoTop,
            topSpacer,
            bottom,
            bottomSpacer
        )

        layout.padding = Insets(25.0)
        layout.alignment = Pos.CENTER
        layout.style = "-fx-background-color: #123D2B;"

        return layout
    }
}
