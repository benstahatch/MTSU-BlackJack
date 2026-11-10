package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage

/** Temporary guest entry screen. Account persistence/authentication is not connected yet. */
class CreateUser(private val casinoClient: CasinoClient) {
    fun getView(stage: Stage): VBox {
        val title = Label("Create Account").apply { font = Font.font(32.0) }
        val usernameField = TextField().apply { promptText = "Enter username"; maxWidth = 300.0 }
        val passwordField = PasswordField().apply { promptText = "Enter password"; maxWidth = 300.0 }
        val message = Label()
        val createButton = Button("Continue as Guest").apply { prefWidth = 300.0 }
        val backButton = Button("Back to Login").apply { prefWidth = 300.0 }

        createButton.setOnAction {
            val username = usernameField.text.trim()
            if (username.isBlank() || passwordField.text.isBlank()) {
                message.text = "Please enter a username and password."
                message.textFill = Color.RED
            } else if (!casinoClient.connect()) {
                message.text = "Could not connect to Table21 server."
                message.textFill = Color.RED
            } else {
                message.text = "Connected. Account saving is not enabled yet; continuing as a guest."
                message.textFill = Color.GREEN
                val lobby = LobbyView(casinoClient)
                stage.scene = Scene(lobby.getView(username, stage), 800.0, 600.0)
            }
        }
        backButton.setOnAction { stage.scene = Scene(LoginView(casinoClient).getView(stage), 800.0, 600.0) }

        return VBox(10.0, title, Label("Username"), usernameField, Label("Password"),
            passwordField, createButton, backButton, message).apply {
            alignment = Pos.CENTER
            padding = Insets(30.0)
            style = "-fx-background-color: #123D2B;"
            children.filterIsInstance<Label>().forEach { it.textFill = Color.WHITE }
            title.textFill = Color.GOLD
        }
    }
}
