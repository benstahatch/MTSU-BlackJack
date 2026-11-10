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

class LoginView(private val casinoClient: CasinoClient) {
    fun getView(stage: Stage): VBox {
        val title = Label("Casino Login").apply { font = Font.font(32.0) }
        val usernameLabel = Label("Username")
        val usernameField = TextField().apply { promptText = "Enter username"; maxWidth = 300.0 }
        val passwordLabel = Label("Password")
        val passwordField = PasswordField().apply { promptText = "Enter password"; maxWidth = 300.0 }
        val loginButton = Button("LOGIN").apply { prefWidth = 300.0 }
        val createUserButton = Button("Create Account").apply { prefWidth = 300.0 }
        val adminButton = Button("ADMIN").apply { prefWidth = 300.0 }
        val quitButton = Button("QUIT").apply { prefWidth = 300.0 }
        val message = Label()

        loginButton.setOnAction {
            val username = usernameField.text.trim()
            if (username.isBlank() || passwordField.text.isBlank()) {
                message.text = "Please enter your username and password."
                message.textFill = Color.RED
            } else if (!casinoClient.connect()) {
                message.text = "Could not connect to Table21 server at the configured address."
                message.textFill = Color.RED
            } else {
                message.text = "Connected to server."
                message.textFill = Color.GREEN
                val lobby = LobbyView(casinoClient)
                stage.scene = Scene(lobby.getView(username, stage), 800.0, 600.0)
            }
        }

        createUserButton.setOnAction {
            stage.scene = Scene(CreateUser(casinoClient).getView(stage), 800.0, 600.0)
        }
        adminButton.setOnAction {
            if (!casinoClient.isConnected() && !casinoClient.connect()) {
                message.text = "Could not connect to server."
                message.textFill = Color.RED
            } else {
                stage.scene = Scene(AdminView(casinoClient).getView(stage), 800.0, 600.0)
            }
        }
        quitButton.setOnAction {
            casinoClient.disconnect()
            javafx.application.Platform.exit()
        }

        return VBox(10.0, title, usernameLabel, usernameField, passwordLabel,
            passwordField, loginButton, createUserButton, adminButton, quitButton, message).apply {
            alignment = Pos.CENTER
            padding = Insets(30.0)
            style = "-fx-background-color: #123D2B;"
            children.filterIsInstance<Label>().forEach { it.textFill = Color.WHITE }
            title.textFill = Color.GOLD
        }
    }
}
