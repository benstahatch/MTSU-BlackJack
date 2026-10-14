package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
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
        val table = Label("Welcome to the casino")
        val username = Label("Welcome, $userName")
        //need to link chips into player system
        val chips = Label("Chips: TBD")
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
